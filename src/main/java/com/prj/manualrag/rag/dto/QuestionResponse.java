package com.prj.manualrag.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record QuestionResponse(
        @Schema(description = "LLM 답변")
        String answer

) {
}