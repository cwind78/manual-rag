package com.prj.manualrag.ai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LiteLlmChatModelProvider
        implements ChatModelProvider {

    private final OpenAiChatModel chatModel;

    @Override
    public AiProvider provider() {
        return AiProvider.LITELLM;
    }

    @Override
    public ChatModel chatModel() {
        return chatModel;
    }

}