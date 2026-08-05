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
        String route = decision.routes().isEmpty() ? "GENERAL" : decision.routes().get(0);
        return switch (route.toUpperCase()) {
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
                                사용자의 질문을 해결하기 위해 필요한 처리 경로를 선택한다.

                                매우 중요:
                                - routes는 사용 가능한 모든 경로 목록이 아니다.
                                - 사용자의 질문에 실제로 필요한 경로만 선택한다.
                                - 질문 하나에는 보통 routes 한 개만 반환한다.
                                - 문서 검색과 웹 검색이 모두 있어야 답할 수 있는 질문처럼 두 경로가
                                  실제로 모두 필요할 때만 두 개 이상 반환한다.
                                - 단순히 판단이 어렵다는 이유로 DOCUMENT, WEB, MCP, GENERAL을
                                  모두 반환하지 않는다.
                                - 질문에 근거가 부족하면 가장 적합한 한 개를 선택한다.
                                - routes와 candidates의 route 목록은 반드시 동일해야 한다.
                                - candidates에는 선택한 각 route를 정확히 한 번씩만 넣는다.
                                - confidence는 해당 route가 질문 해결에 필요한 정도이며 0.0~1.0이다.

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

                                아래 후보는 선택 가능한 기능 참고자료일 뿐이다. 후보에 있다고 해서
                                해당 route를 자동으로 선택하지 않는다.

                                관련 기능 후보:
                                %s

                                출력 규칙:
                                - 반드시 JSON 객체 하나만 출력한다.
                                - routes는 ["DOCUMENT"], ["WEB"], ["MCP"], ["GENERAL"] 중 하나이거나,
                                  실제로 두 경로가 모두 필요한 경우에만 두 개 이상의 배열이다.
                                - routes 배열의 순서는 적합도가 높은 순서다.
                                - candidates 배열 개수와 routes 배열 개수는 항상 같다.

                                예시 1 - 문서만 필요한 질문:
                                질문: "사용자 설명서에서 필터 교체 방법을 알려줘"
                                {"routes":["DOCUMENT"],"confidence":0.96,"candidates":[{"route":"DOCUMENT","confidence":0.96,"reason":"사용자 설명서 검색이 필요함"}]}

                                예시 2 - 웹만 필요한 질문:
                                질문: "오늘 서울 날씨 알려줘"
                                {"routes":["WEB"],"confidence":0.98,"candidates":[{"route":"WEB","confidence":0.98,"reason":"최신 정보가 필요함"}]}

                                예시 3 - MCP만 필요한 질문:
                                질문: "내 GitHub 프로필을 보여줘"
                                {"routes":["MCP"],"confidence":0.99,"candidates":[{"route":"MCP","confidence":0.99,"reason":"GitHub 계정 기능 호출이 필요함"}]}

                                예시 4 - 두 경로가 모두 필요한 질문:
                                질문: "내 설명서의 제품 가격과 현재 인터넷 판매 가격을 비교해줘"
                                {"routes":["DOCUMENT","WEB"],"confidence":0.91,"candidates":[{"route":"DOCUMENT","confidence":0.91,"reason":"설명서 가격 확인"},{"route":"WEB","confidence":0.89,"reason":"현재 인터넷 가격 확인"}]}

                                이제 사용자 질문을 분류하고 JSON만 출력한다.
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
            IntentDecision decision = objectMapper.readValue(json, IntentDecision.class);
            List<IntentDecision.RouteCandidate> normalizedCandidates = decision.candidates() == null
                    ? List.of()
                    : decision.candidates();
            if (normalizedCandidates.size() < decision.routes().size()) {
                List<IntentDecision.RouteCandidate> existingCandidates = normalizedCandidates;
                normalizedCandidates = decision.routes().stream()
                        .map(route -> existingCandidates.stream()
                                .filter(candidate -> route.equalsIgnoreCase(candidate.route()))
                                .findFirst()
                                .orElse(new IntentDecision.RouteCandidate(route, decision.confidence(), "LLM selected route")))
                        .toList();
            }
            return new IntentDecision(decision.routes(), decision.confidence(), normalizedCandidates);
        } catch (Exception e) {
            log.warn("Intent JSON parse failed; fallback to GENERAL. result={}", result, e);
            return new IntentDecision(List.of("GENERAL"), 0.0, List.of());
        }
    }
}
