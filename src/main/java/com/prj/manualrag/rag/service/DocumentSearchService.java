package com.prj.manualrag.rag.service;

import com.prj.manualrag.rag.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentSearchService {

    private final VectorStore vectorStore;
    private final KeywordSearchService keywordSearchService;

    public SearchResult search(String question, List<String> selectedFiles){
        String filter = selectedFiles.stream()
                .map(f -> "fileName == '" + f + "'")
                .collect(Collectors.joining(" or "));

        log.info("===============fileName: {}", filter);

        List<Document> semanticDocuments =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(question)
                                .topK(10)
                                .filterExpression(filter)
                                .similarityThreshold(0.0)
                                .build()
                );

        List<Document> keywordDocuments =
                keywordSearchService.search(question);

        List<Document> documents =
                mergeDocuments(
                        semanticDocuments,
                        keywordDocuments
                );

        log.info("Hybrid 검색 결과={}", documents.size());

        String context =
                documents.stream()
                        .map(Document::getText)
                        .reduce("", (a,b)->a+"\n\n"+b);

        return new SearchResult(
                context,
                documents
        );
    }

    private List<Document> mergeDocuments(
            List<Document> semantic,
            List<Document> keyword){

        Map<String,Document> result =
                new LinkedHashMap<>();

        semantic.forEach(d->result.put(d.getId(),d));

        keyword.forEach(d->result.put(d.getId(),d));

        return result.values()
                .stream()
                .limit(20)
                .toList();
    }

}
