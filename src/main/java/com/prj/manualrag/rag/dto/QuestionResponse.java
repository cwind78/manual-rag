package com.prj.manualrag.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record QuestionResponse(
        @Schema(description = "응답 상태")
        String status,
        @Schema(description = "LLM 답변")
        String answer,
        String question,
        java.util.List<QuestionOption> options

) {
    public static QuestionResponse answer(String answer) {
        return new QuestionResponse("ANSWER", answer, null, null);
    }

    public static QuestionResponse confirmation(
            String question,
            java.util.List<QuestionOption> options
    ) {
        return new QuestionResponse("NEED_CONFIRMATION", null, question, options);
    }
}
