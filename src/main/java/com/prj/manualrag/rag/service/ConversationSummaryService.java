package com.prj.manualrag.rag.service;

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
public class ConversationSummaryService {
    private final ChatMemory chatMemory;
    private final ChatClient chatClient;
    private final ConversationSummaryStore store;

    public void summarize(String conversationId) {
        summarize(conversationId, null, null);
    }

    /**
     * The memory advisor persists the current turn after the ChatClient chain
     * returns. Include the current turn explicitly so the summary is not one
     * request behind.
     */
    public void summarize(String conversationId, String currentQuestion, String currentAnswer) {
        List<Message> messages = chatMemory.get(conversationId);
        int currentTurnMessages = currentQuestion == null || currentAnswer == null ? 0 : 2;
        log.info("conversation memory: conversationId={}, storedMessages={}, currentTurnMessages={}",
                conversationId, messages.size(), currentTurnMessages);
        if(messages.size() + currentTurnMessages < 4) {
            return;
        }

        String text = messages.stream()
                        .map(Message::getText)
                        .reduce("", (a,b)->a+"\n"+b);
        if (currentQuestion != null && currentAnswer != null) {
            text += "\n사용자: " + currentQuestion + "\nAI: " + currentAnswer;
        }
        String summary = chatClient
                        .prompt()
                        .user(
                                """
                                아래 대화를 요약하세요.
        
                                중요한 정보만 유지하세요.
                                제품명, 대상, 사용 목적,
                                사용자가 원하는 작업을 포함하세요.
        
                                대화:
                                %s
                                """
                                        .formatted(text)
                        )
                        .call()
                        .content();
        log.info("summary={}", summary);
        store.save(
                conversationId,
                summary
        );
    }
}
