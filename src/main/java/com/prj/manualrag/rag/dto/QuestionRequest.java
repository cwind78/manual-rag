package com.prj.manualrag.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record QuestionRequest(
        @NotBlank(message = "질문은 필수입니다.")
        @Schema(
                description = "사용자 질문",
                example = "필터 청소는 어떻게 하나요?"
        )
        String question
) {
}
