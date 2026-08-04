package com.prj.manualrag.rag.service;

import com.prj.manualrag.rag.config.SearxngProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchService {
    private final SearxngProperties properties;
    private final WebClient webClient;

    public String search(String question){
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(properties.getUrl())
                    .queryParam("q", question)
                    .queryParam("language", "ko")
                    .queryParam("engines", "google")
                    .queryParam("categories", "general")
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();

            log.info("SearXNG request URL: {}", url);

            String html = webClient
                            .get()
                            .uri(url)
                            .header(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                            .header(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR,ko;q=0.9,en;q=0.8")
                            .accept(MediaType.TEXT_HTML)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();

            log.info("SearXNG HTML response length: {}", html == null ? 0 : html.length());
            log.info("html: {}", html);

            return parse(html);
        } catch(Exception e){
            log.error("SearXNG 검색 실패", e);
            return "";
        }
    }

    private String parse(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        Document document = Jsoup.parse(html);
        StringBuilder result = new StringBuilder();

        for (Element item : document.select("article.result")) {
            Element title = item.selectFirst("h3 a");
            Element content = item.selectFirst("p.content");
            Element link = item.selectFirst("h3 a");

            result.append("제목: ")
                    .append(title == null ? "" : title.text())
                    .append("\n");
            result.append("내용: ")
                    .append(content == null ? "" : content.text())
                    .append("\n");
            result.append("주소: ")
                    .append(link == null ? "" : link.absUrl("href"))
                    .append("\n\n");
        }
        return result.toString();
    }
}
