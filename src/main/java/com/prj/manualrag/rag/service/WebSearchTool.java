package com.prj.manualrag.rag.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSearchTool {
    private final WebSearchService webSearchService;

    public String search(String question){
        return webSearchService.search(question);
    }
}
