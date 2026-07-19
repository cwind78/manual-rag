package com.prj.manualrag.rag.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeywordSearchService {


    private final JdbcTemplate jdbcTemplate;


    public List<Document> search(String keyword){


        String sql =
                """
                SELECT content
                  FROM vector_store
                 WHERE content LIKE ?
                 LIMIT 10
                """;


        return jdbcTemplate.query(
                sql,
                (rs,row)->
                        new Document(
                                rs.getString("content")
                        ),
                keyword
        );

    }
}
