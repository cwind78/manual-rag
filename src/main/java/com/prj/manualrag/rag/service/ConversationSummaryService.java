package com.prj.manualrag.rag.service;

import com.prj.manualrag.rag.memory.ConversationSummaryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationSummaryService {
    private final ChatMemory chatMemory;
    private final ChatClient chatClient;
    private final ConversationSummaryStore store;

    public void summarize(String conversationId) {
        List<Message> messages = chatMemory.get(conversationId);
        if(messages.size() < 8) {
            return;
        }

        String text = messages.stream()
                        .map(Message::getText)
                        .reduce("", (a,b)->a+"\n"+b);
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

        store.save(
                conversationId,
                summary
        );
    }
}