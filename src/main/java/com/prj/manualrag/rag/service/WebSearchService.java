package com.prj.manualrag.rag.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prj.manualrag.rag.config.SearxngProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchService {
    private final SearxngProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public String search(String question){
        try {
            String encoded = URLEncoder.encode(question, StandardCharsets.UTF_8);
            String json = webClient
                            .get()
                            .uri(
                                    properties.getUrl()
                                            + "?q="
                                            + encoded
                                            + "&format=json"
                            )
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();

            return parse(json);
        } catch(Exception e){
            log.error("SearXNG 검색 실패", e);
            return "";
        }
    }

    private String parse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        StringBuilder result = new StringBuilder();

        for(JsonNode item : root.path("results")) {
            result.append("제목: ")
                    .append(item.path("title").asText())
                    .append("\n");
            result.append("내용: ")
                    .append(item.path("content").asText())
                    .append("\n");
            result.append("주소: ")
                    .append(item.path("url").asText())
                    .append("\n\n");
        }
        return result.toString();
    }
}