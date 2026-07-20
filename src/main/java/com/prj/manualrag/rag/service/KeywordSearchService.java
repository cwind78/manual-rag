package com.prj.manualrag.rag.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordSearchService {
    private final JdbcTemplate jdbcTemplate;

    public List<Document> search(String keyword, List<String> selectedFiles) {
        log.info("===============list={}", selectedFiles);
        StringBuilder sql = new StringBuilder(
                """
                SELECT content
                  FROM vector_store
                 WHERE content LIKE ?
                 """);

        List<Object> params = new ArrayList<>();
        params.add("%" + keyword + "%");

        if (selectedFiles != null && !selectedFiles.isEmpty()) {
            String placeholders = selectedFiles.stream()
                    .map(file -> "?")
                    .collect(Collectors.joining(","));

            sql.append("""
                AND metadata::jsonb->>'fileName' IN (
                """)
                    .append(placeholders)
                    .append(") ");

            params.addAll(selectedFiles);
        }

        sql.append("""
            LIMIT 10
            """);

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) ->
                                new Document(rs.getString("content")
                            )
        );
    }
}