package com.prj.manualrag.rag.service;

import com.prj.manualrag.rag.config.SearxngProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchService {
    private final SearxngProperties properties;
    private final WebClient webClient;
    private final ChatClient chatClient;

    public String search(String question) {
        try {
            // SearXNG 차단을 피하기 위해 검색 요청은 질문당 항상 한 번만 보낸다.
            String query = createSearchQuery(question);
            Map<String, SearchItem> items = new LinkedHashMap<>();
            fetchSearchResults(query).forEach(item -> items.putIfAbsent(item.url(), item));

            // 본문을 가져올 후보 수를 제한해 응답 시간과 외부 사이트 부하를 관리한다.
            List<SearchItem> documents = items.values().stream().limit(10)
                    .map(this::withBody).filter(item -> !item.body().isBlank()).toList();
            if (documents.isEmpty()) {
                return items.values().stream().limit(8).map(SearchItem::asText)
                        .reduce("", (a, b) -> a + b + "\n");
            }

            List<SearchItem> ranked = rerank(question, documents);
            StringBuilder result = new StringBuilder("검색 근거 문서:\n");
            ranked.stream().limit(5).forEach(item -> result.append(item.asText()).append("\n"));
            return result.toString();
        } catch (Exception e) {
            log.error("웹 검색 실패", e);
            return "웹 검색 중 오류가 발생했습니다.";
        }
    }

    private String createSearchQuery(String question) {
        try {
            String answer = chatClient.prompt().user("""
                    다음 질문의 의도를 가장 잘 반영하는 웹 검색 질의 하나를 작성하라.
                    핵심 대상, 조건, 제품명·버전·오류명 등 검색에 중요한 표현은 유지하라.
                    너무 일반적인 표현은 제거하고, 검색엔진에 그대로 전달할 한 줄만 출력하라.
                    설명, 번호, 따옴표, 여러 질의를 출력하지 마라.
                    질문: %s
                    """.formatted(question)).call().content();
            String query = Optional.ofNullable(answer).orElse("").split("\\R")[0]
                    .replaceFirst("^\\s*[-*0-9.)]+\\s*", "").trim();
            return query.isBlank() ? question : query;
        } catch (Exception e) {
            log.warn("검색어 생성 실패. 원문 질의를 사용합니다.", e);
            return question;
        }
    }

    private List<SearchItem> fetchSearchResults(String query) throws Exception {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getUrl())
                    .queryParam("q", query).queryParam("language", "ko")
                    .queryParam("engines", "google").queryParam("categories", "general")
                    .queryParam("format", "html").build().encode(StandardCharsets.UTF_8).toUri();
        String html = webClient.get().uri(uri).header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                .header(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9,en;q=0.8")
                .accept(MediaType.TEXT_HTML).retrieve().bodyToMono(String.class).block();
        org.jsoup.nodes.Document document = Jsoup.parse(Optional.ofNullable(html).orElse(""));
        List<SearchItem> items = new ArrayList<>();
        for (org.jsoup.nodes.Element item : document.select("article.result")) {
            org.jsoup.nodes.Element link = item.selectFirst("h3 a");
            if (link != null) items.add(new SearchItem(link.text(),
                    Optional.ofNullable(item.selectFirst("p.content")).map(org.jsoup.nodes.Element::text).orElse(""),
                    link.absUrl("href"), ""));
        }
        return items;
    }

    private SearchItem withBody(SearchItem item) {
        try {
            URI uri = URI.create(item.url());
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) return item;
            String html = webClient.get().uri(uri).header(HttpHeaders.USER_AGENT, "manual-rag/1.0")
                    .accept(MediaType.TEXT_HTML).retrieve().bodyToMono(String.class).block();
            String body = Jsoup.parse(Optional.ofNullable(html).orElse("")).body().text();
            return new SearchItem(item.title(), item.snippet(), item.url(), body.substring(0, Math.min(body.length(), 8000)));
        } catch (Exception e) {
            log.debug("본문 추출 실패: {}", item.url());
            return item;
        }
    }

    private List<SearchItem> rerank(String question, List<SearchItem> documents) {
        String candidates = documents.stream().map(item -> "URL: " + item.url() + "\n본문: " + item.body()).reduce("", (a, b) -> a + "\n---\n" + b);
        try {
            String selected = chatClient.prompt().user("""
                    질문에 답하는 데 가장 유용한 문서의 URL만 최대 5개 골라라.
                    반드시 아래 URL 중 하나를 한 줄에 하나씩 출력하고, 설명은 하지 마라.
                    질문: %s
                    문서:
                    %s
                    """.formatted(question, candidates)).call().content();
            Set<String> urls = new LinkedHashSet<>(Arrays.asList(Optional.ofNullable(selected).orElse("").split("\\R")));
            List<SearchItem> ranked = documents.stream().filter(item -> urls.stream().anyMatch(item.url()::contains)).toList();
            return ranked.isEmpty() ? documents : ranked;
        } catch (Exception e) {
            log.warn("웹 문서 리랭킹 실패. 검색 순서를 유지합니다.", e);
            return documents;
        }
    }

    private record SearchItem(String title, String snippet, String url, String body) {
        String asText() { return "제목: " + title + "\n내용: " + (body.isBlank() ? snippet : body) + "\n주소: " + url + "\n"; }
    }
}
