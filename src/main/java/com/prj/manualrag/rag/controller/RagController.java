package com.prj.manualrag.rag.controller;

import com.prj.manualrag.common.dto.ApiResponse;
import com.prj.manualrag.rag.dto.QuestionRequest;
import com.prj.manualrag.rag.dto.QuestionResponse;
import com.prj.manualrag.rag.service.RagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Tag(name = "RAG")
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {
    private final RagService ragService;

    @Operation(
            summary = "질문하기",
            description = "설명서를 검색하여 답변합니다."
    )
    @PostMapping("/question")
    public ResponseEntity<ApiResponse<QuestionResponse>> question(
            @Valid @RequestBody QuestionRequest request
    ){
        return ResponseEntity.ok(ApiResponse.success(ragService.answer(
                request.question(),
                request.conversationId())));
    }

    @PostMapping(value = "/question/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamQuestion(
            @Valid @RequestBody QuestionRequest request
    ) {
        return ragService.streamAnswer(
                        request.question(), request.conversationId())
                .map(token -> ServerSentEvent.<String>builder()
                        .event("message").data(token).build())
                .concatWithValues(ServerSentEvent.<String>builder()
                        .event("done").data("[DONE]").build());
    }
}
