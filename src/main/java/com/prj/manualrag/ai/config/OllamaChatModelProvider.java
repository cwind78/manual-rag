package com.prj.manualrag.ai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OllamaChatModelProvider
        implements ChatModelProvider {

    private final OllamaChatModel chatModel;

    @Override
    public AiProvider provider() {
        return AiProvider.OLLAMA;
    }

    @Override
    public ChatModel chatModel() {
        return chatModel;
    }

}