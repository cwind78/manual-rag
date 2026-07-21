package com.prj.manualrag.rag.service;

import com.prj.manualrag.rag.domain.Intent;
import com.prj.manualrag.rag.dto.QuestionResponse;
import com.prj.manualrag.rag.memory.ConversationSummaryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {
    private final ChatClient chatClient;
    private final IntentClassifier intentClassifier;
    private final DocumentSearchTool documentSearchTool;
    private final WebSearchTool webSearchTool;
    private final ChatMemory chatMemory;
    private final ConversationSummaryStore summaryStore;
    private final ConversationSummaryService summaryService;
    private final DocumentSelectorService documentSelectorService;

    public QuestionResponse answer(String question, String conversationId) {
        log.info(
                "===============conversationId={}, question={}",
                conversationId,
                question
        );

        String summary =
                summaryStore.get(
                        conversationId
                );
        log.info("===============summary={}", summary);

        String searchQuestion =
                rewriteQuestion(
                        question,
                        summary
                );

        Intent intent = intentClassifier.classify(question);
        String context = "";
        if(intent == Intent.DOCUMENT) {
//            context = documentSearchTool.search(searchQuestion);
            List<String> selectedFiles = documentSelectorService.select(searchQuestion);
            if(selectedFiles.isEmpty()) {
                context = documentSearchTool.search(searchQuestion, null);
            } else {
                context = documentSearchTool.search(searchQuestion, selectedFiles);
            }
        } else if(intent == Intent.WEB) {
            context = webSearchTool.search(searchQuestion);
        }

        log.info("Selected intent: {}", intent.toString());
        String prompt = """
                당신은 한국어 AI Assistant이다.
                답변 규칙:
                - 자료 내용이 있으면 반드시 자료를 근거로 답한다.
                - 자료 내용이 없으면 일반 지식으로 답할 수 있다.
                - 자료와 일반 지식이 충돌하면 자료를 우선한다.

                자료 내용:
                %s

                질문:
                %s
                
                - 한글로만 답해라
                """
                        .formatted(context, searchQuestion);

        List<Message> messages =
                chatMemory.get(
                        conversationId
                );

        log.info("===============memory size={}", messages.size());
        messages.forEach(message ->
                log.info(
                        "===============memory role={}, content={}",
                        message.getMessageType(),
                        message.getText()
                )
        );

        String answer = chatClient
                        .prompt()
                        .user(prompt)
                        .advisors(
                                advisor -> advisor.param(
                                        ChatMemory.CONVERSATION_ID,
                                        conversationId
                                )
                        )
                        .call()
                        .content();

        // 여기서 Memory 확인
        messages = chatMemory.get(conversationId);

        log.info("===== MEMORY AFTER CALL =====");
        messages.forEach(message ->
                log.info(
                        "===============role={}, content={}",
                        message.getMessageType(),
                        message.getText()
                )
        );

        summaryService.summarize(
                conversationId
        );

        return new QuestionResponse(answer);

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