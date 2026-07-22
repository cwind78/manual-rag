package com.prj.manualrag.ai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatModelFactory {

    private final AiProperties properties;

    private final List<ChatModelProvider> providers;

    public ChatModel getChatModel() {

        return providers.stream()
                .filter(p -> p.provider() == properties.getProvider())
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "지원하지 않는 Provider : "
                                        + properties.getProvider()
                        ))
                .chatModel();

    }

}