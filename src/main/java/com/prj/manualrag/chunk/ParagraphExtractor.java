package com.prj.manualrag.chunk;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ParagraphExtractor {
    /**
     * 페이지 Document 목록을 문단 목록으로 변환
     */
    public List<Paragraph> extract(List<Document> documents) {
        List<Paragraph> paragraphs = new ArrayList<>();

        for (Document document : documents) {
            String text = document.getText();
            if (text == null || text.isBlank()) {
                continue;
            }

            Map<String, Object> metadata = document.getMetadata();
            String[] split = text.split("\\R\\s*\\R");

            for (String paragraphText : split) {
                String cleaned = clean(paragraphText);
                if (isValid(cleaned)) {
                    paragraphs.add(
                            Paragraph.builder()
                                    .text(cleaned)
                                    .metadata(metadata)
                                    .build()
                    );
                }
            }
        }

        return paragraphs;
    }

    private String clean(String text) {
        return text
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 너무 작은 텍스트 제거
     *
     * 예:
     * -
     * 1.
     * 페이지 번호
     */
    private boolean isValid(String text) {
        if (text.isBlank()) {
            return false;
        }

        return text.length() >= 10;
    }
}
