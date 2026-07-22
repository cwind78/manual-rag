package com.prj.manualrag.ai.config;

import org.springframework.ai.chat.model.ChatModel;

public interface ChatModelProvider {
    AiProvider provider();
    ChatModel chatModel();
}
