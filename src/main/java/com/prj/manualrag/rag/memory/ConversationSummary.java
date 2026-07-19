package com.prj.manualrag.rag.memory;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConversationSummary {
    private String conversationId;
    private String summary;
}
