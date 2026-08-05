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

        //이전 요청까지의 대화 요약과 방금 받은 질문 합치기
        String searchQuestion =
                rewriteQuestion(
                        question,
                        summary
                );

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

        // 사용자가 경로를 직접 선택했다면 그 경로를 사용하고,
        // 그렇지 않으면 LLM이 판단한 모든 경로를 실행한다.
        //사용자가 의도를 선택하는 경우를 없앴기 때문에 decision.routes()가 의도가 된다.
        List<String> executionRoutes = selectedRoutes != null && !selectedRoutes.isEmpty()
                ? selectedRoutes
                : decision.routes();
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

                질문:
                %s
                
                - 한글로만 답해라
                """
                        .formatted(String.join(", ", executionRoutes), context, searchQuestion);

//        List<Message> messages =
//                chatMemory.get(
//                        conversationId
//                );

//        log.info("===============memory size={}", messages.size());
//        messages.forEach(message ->
//                log.info(
//                        "===============memory role={}, content={}",
//                        message.getMessageType(),
//                        message.getText()
//                )
//        );

        List<ToolCallback> mcpTools =
                externalToolProvider.activeTools();

//        String answer = chatClient
//                        .prompt()
//                        .user(prompt)
//                        .toolCallbacks(mcpTools)
//                        .advisors(
//                                advisor -> advisor.param(
//                                        ChatMemory.CONVERSATION_ID,
//                                        conversationId
//                                )
//                        )
//                        .call()
//                        .content();

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

        // 여기서 Memory 확인
//        messages = chatMemory.get(conversationId);

//        log.info("===== MEMORY AFTER CALL =====");
//        messages.forEach(message ->
//                log.info(
//                        "===============role={}, content={}",
//                        message.getMessageType(),
//                        message.getText()
//                )
//        );

        summaryService.summarize(conversationId, question, answer);

        return QuestionResponse.answer(answer);

//        String answer =
//                chatClient
//                        .prompt()
//                        .system("""
//                                당신은 문서 검색 기능을 사용할 수 있는 AI Assistant이다.
//
//                                                    규칙
//
//                                                    1. 사용자가 업로드한 문서에 대한 질문이면 반드시 Tool을 사용한다.
//
//                                                    2. Tool이 반환한 내용을 근거로 최종 답변을 작성한다.
//
//                                                    3. "검색하겠습니다.", "찾아보겠습니다." 같은 중간 과정을 사용자에게 말하지 않는다.
//
//                                                    4. Tool의 반환 내용을 그대로 인용하지 말고 자연스럽게 답변한다.
//
//                                                    5. Tool에서 충분한 정보를 찾지 못하거나 문서에 대한 질문이 아니면 추론하여 답한다.
//                                                    단 Tool을 사용했지만 충분한 정보를 찾지 못한경우 "업로드된 문서에서 확인할 수 없습니다."
//                                                    라고 답한 다음에 추론하여 답한다.
//
//                                                    6. 한글로만 답한다.
//""")
//                        .tools(documentSearchTool)
//                        .user(question)
//                        .call()
//                        .content();
//
//        return new QuestionResponse(answer);
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


        return chatClient
                .prompt()
                .user(
                        """
                        이전 대화 내용을 참고해서
                        검색하기 좋은 질문으로 변경하세요.
            
                        이전 대화:
                        %s
            
                        현재 질문:
                        %s
            
                        검색 질문만 출력하세요.
                        """
                                .formatted(
                                        summary,
                                        question
                                )
                )
                .call()
                .content();

    }
}
