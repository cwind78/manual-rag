package com.prj.manualrag.rag.service;

import com.prj.manualrag.rag.domain.Intent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.ai.document.Document;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prj.manualrag.rag.dto.IntentDecision;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentClassifier {
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    public Intent classify(String question){
        return classify(question, List.of());
    }

    public Intent classify(String question, List<Document> candidates){
        IntentDecision decision = decide(question, candidates);
        return switch (decision.route().toUpperCase()) {
            case "DOCUMENT" -> Intent.DOCUMENT;
            case "WEB" -> Intent.WEB;
            case "MCP" -> Intent.MCP;
            default -> Intent.GENERAL;
        };
    }

    public IntentDecision decide(String question, List<Document> candidates){
        String result =
                chatClient
                        .prompt()
                        .system("""
                                사용자의 질문 의도를 분류한다.

                                DOCUMENT:
                                업로드된 문서,
                                제품 설명서,
                                규정,
                                계약서,
                                PDF,
                                사내 자료와 관련된 질문

                                WEB:
                                최신 정보,
                                현재 가격,
                                뉴스,
                                오늘 날씨,
                                최근 사건,
                                인터넷 정보가 필요한 질문

                                GENERAL:
                                일반 상식,
                                일상 질문

                                MCP:
                                GitHub, 외부 서비스, 계정 정보 조회 등 등록된 외부 기능이 필요한 질문

                                관련 기능 후보:
                                %s

                                반드시 다음 JSON만 출력한다.
                                {"route":"DOCUMENT|WEB|MCP|GENERAL","confidence":0.0,
                                "candidates":[{"route":"DOCUMENT","confidence":0.0,"reason":""}]}
                                """.formatted(candidates.stream()
                                .map(document -> document.getMetadata().get("capabilityType")
                                        + " / " + document.getMetadata().get("capabilityName")
                                        + " / " + document.getText())
                                .reduce("", (a, b) -> a + "\n" + b)))
                        .user(question)
                        .call()
                        .content();

        log.info("intent decision result={}", result);
        try {
            String json = result.substring(result.indexOf('{'), result.lastIndexOf('}') + 1);
            return objectMapper.readValue(json, IntentDecision.class);
        } catch (Exception e) {
            log.warn("Intent JSON parse failed; fallback to GENERAL. result={}", result, e);
            return new IntentDecision("GENERAL", 0.0, List.of());
        }
    }
}
