package com.prj.manualrag.rag.memory;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConversationSummaryStore {
    private final Map<String, String> store = new ConcurrentHashMap<>();
    public void save(String conversationId, String summary) {
        store.put(conversationId, summary);
    }

    public String get(String conversationId){
        return store.getOrDefault(conversationId, "");
    }
}
