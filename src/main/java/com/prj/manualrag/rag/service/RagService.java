package com.prj.manualrag.rag.service;

import com.prj.manualrag.rag.domain.Intent;
import com.prj.manualrag.rag.dto.QuestionResponse;
import com.prj.manualrag.rag.memory.ConversationSummaryStore;
import com.prj.manualrag.agent.port.ExternalToolProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import com.prj.manualrag.capability.CapabilitySearchService;
import org.springframework.ai.document.Document;
import com.prj.manualrag.rag.dto.IntentDecision;


@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {
    private final ChatClient chatClient;
    private final IntentClassifier intentClassifier;
    private final DocumentSearchTool documentSearchTool;
    private final WebSearchTool webSearchTool;
    private final ConversationSummaryStore summaryStore;
    private final ConversationSummaryService summaryService;
    private final DocumentSelectorService documentSelectorService;
    private final ExternalToolProvider externalToolProvider;
    private final CapabilitySearchService capabilitySearchService;

    public QuestionResponse answer(String question, String conversationId, List<String> selectedRoutes) {
        String summary =
                summaryStore.get(
                        conversationId
                );
        //이전 요청까지의 대화 요약 가져오기
        log.info("conversation summary: conversationId={}, present={}, length={}",
                conversationId, !summary.isBlank(), summary.length());

        // 현재 질문을 항상 기준으로 삼고, 현재 질문이 불완전한 후속 질문일 때만
        // 이전 요약으로 검색어를 보완한다.
        String searchQuestion =
                rewriteQuestion(
                        question,
                        summary
                );
        log.info("Current question={}, search question={}", question, searchQuestion);

        //질문에 해당하는 실행할 기능 목록(벡터 스토어에 저장되어 있는 기능 가져오기)
        // SELECT
        //        id,
        //        content,
        //        metadata
        //   FROM vector_store
        //  WHERE metadata ->> 'capability' = 'true';
        List<Document> capabilityCandidates =
                capabilitySearchService.search(searchQuestion, 8);
        log.info("Capability candidates: question={}, candidates={}",
                searchQuestion, capabilityCandidates.stream()
                        .map(document -> document.getMetadata().get("capabilityName"))
                        .toList());

        //의도 가져오기
        //WEB|DOCUMENT|MCP|GENERAL 중에 1개 이상의 routes와 route별 capabilities(1.0 이하의 소수값)
        IntentDecision decision = intentClassifier.decide(searchQuestion, capabilityCandidates);
        //selectedRoutes는 사용자와 상호작용하기 위해 받았던 것으로 현재는 사용자가 의도를 선택하지 않기 때문에 항상 null이다. 즉 decision.routes()가 유사도가 가장 높은 의도가 된다.
        List<String> executionRoutes = selectedRoutes != null && !selectedRoutes.isEmpty()
                ? selectedRoutes
                : decision.routes();

        //아래도 탈일이 없다. llm이 decision.routes()를 null로 리턴하지 않으면
        if (executionRoutes == null || executionRoutes.isEmpty()) {
            executionRoutes = List.of("GENERAL");
        }

        String selectedRoute = executionRoutes.get(0);
        if (selectedRoute == null || selectedRoute.isBlank()) {
            selectedRoute = "GENERAL";
        }
        Intent intent = switch (selectedRoute.toUpperCase()) {
            case "DOCUMENT" -> Intent.DOCUMENT;
            case "WEB" -> Intent.WEB;
            case "MCP" -> Intent.MCP;
            default -> Intent.GENERAL;
        };
        String context = "";
        //선택된 의도가 있으면 각 의도별 불린 값을 설정한다.
        boolean useDocument = executionRoutes.stream()
                .anyMatch(route -> "DOCUMENT".equalsIgnoreCase(route));
        boolean useWeb = executionRoutes.stream()
                .anyMatch(route -> "WEB".equalsIgnoreCase(route));
        boolean useMcp = executionRoutes.stream()
                .anyMatch(route -> "MCP".equalsIgnoreCase(route));

        if (useDocument || intent == Intent.DOCUMENT) {
//            context = documentSearchTool.search(searchQuestion);
            List<String> selectedFiles = documentSelectorService.select(searchQuestion);
            if(selectedFiles.isEmpty()) {
               //context = documentSearchTool.search(searchQuestion, null);
            } else {
                context = documentSearchTool.search(searchQuestion, selectedFiles);
            }
        }
        if (useWeb || intent == Intent.WEB) {
            context += "\n\n[웹 검색 결과]\n" + webSearchTool.search(searchQuestion);
        }

        log.info("Selected intent: {}", intent.toString());
        String prompt = """
                당신은 한국어 AI Assistant이다.
                답변 규칙:
                - 자료 내용이 있으면 반드시 자료를 근거로 답한다.
                - 자료 내용이 없으면 일반 지식으로 답할 수 있다.
                - 자료와 일반 지식이 충돌하면 자료를 우선한다.
                - 아래에 여러 검색 경로의 결과가 있으면 모든 결과를 종합하여 하나의 답변으로 작성한다.
                - MCP 기능이 선택된 경우 등록된 MCP 도구를 사용하여 얻은 결과를 답변에 반영한다.
                - 검색 결과가 서로 다르면 출처와 근거를 구분해서 설명한다.

                실행 경로:
                %s

                자료 내용:
                %s

                현재 사용자 질문(이 질문에 최우선으로 답변):
                %s

                검색에 사용한 보강 질문(현재 질문이 생략형일 때만 참고):
                %s

                - 한글로만 답해라
                """
                        .formatted(String.join(", ", executionRoutes), context, question, searchQuestion);

        List<ToolCallback> mcpTools =
                externalToolProvider.activeTools();

        ChatResponse response = chatClient
                .prompt()
                .user(prompt)
                .toolCallbacks(mcpTools)
                .advisors(
                        advisor -> advisor.param(
                                ChatMemory.CONVERSATION_ID,
                                conversationId
                        )
                )
                .call()
                .chatResponse();

        log.info(
                "LLM tool call 여부: hasToolCalls={}",
                response.hasToolCalls()
        );

        if (response.hasToolCalls()) {
            response.getResult()
                    .getOutput()
                    .getToolCalls()
                    .forEach(toolCall ->
                            log.info(
                                    "LLM tool call: name={}, arguments={}",
                                    toolCall.name(),
                                    toolCall.arguments()
                            )
                    );
        }

        String answer = response.getResult()
                .getOutput()
                .getText(); //여기까지 테스트

        summaryService.summarize(conversationId, question, answer);

        return QuestionResponse.answer(answer);
    }

    private String routeLabel(String route) {
        return switch (route.toUpperCase()) {
            case "DOCUMENT" -> "문서에서 확인";
            case "WEB" -> "인터넷 검색";
            case "MCP" -> "외부 기능 사용";
            default -> "일반 답변";
        };
    }

    private String rewriteQuestion(
            String question,
            String summary
    ){


        if(summary.isBlank()){
            return question;
        }


        try {
            String rewritten = chatClient
                    .prompt()
                    .user(
                            """
                            현재 질문을 중심으로 웹 검색용 질문으로 준비한다.

                            반드시 지킬 규칙:
                            1. 현재 질문이 독립적으로 이해되는 새 주제이면 현재 질문을 그대로 출력한다..
                            2. 이전 대화 요약은 현재 질문이 '몇 시에?', '그 제품은?', '얼마인가요?'처럼
                               맥락을 이어가는 경우에 사용한다.
                            3. 현재 질문과 이전 요약의 주제가 다르면 이전 요약을 절대 섞지 않는다.
                            4. 이전 요약이 현재 질문을 덮어쓰거나 답변 주제를 바꾸게 해서는 않된다.
                            5. 검색 질문 한 줄만 출력하고 설명, 번호, 따옴표는 출력하지 않는다.

                            예시 1:
                            이전 요약: 사용자는 냉장고 가격을 물었다.
                            현재 질문: 자동차 가격을 알아봐 줘.
                            출력: 자동차 가격을 알아봐 줘.

                            예시 2:
                            이전 요약: 오늘 서울 날씨를 확인했다.
                            현재 질문: 몇 시에?
                            출력: 오늘 서울에 비가 오는 시간은 몇 시인가?

                            이전 대화 요약:
                            %s

                            현재 질문:
                            %s
                            """
                                    .formatted(summary, question)
                    )
                    .call()
                    .content();
            String result = rewritten == null ? "" : rewritten.split("\\R")[0]
                    .replaceFirst("^\\s*[-*0-9.)]+\\s*", "")
                    .trim();
            return result.isBlank() ? question : result;
        } catch (Exception e) {
            log.warn("질문 재작성 실패. 현재 질문을 그대로 사용합니다.", e);
            return question;
        }

    }
}
