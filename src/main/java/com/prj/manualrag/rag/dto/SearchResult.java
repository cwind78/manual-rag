package com.prj.manualrag.rag.dto;

import org.springframework.ai.document.Document;

import java.util.List;

public record SearchResult(

        String context,

        List<Document> documents

) {
}
