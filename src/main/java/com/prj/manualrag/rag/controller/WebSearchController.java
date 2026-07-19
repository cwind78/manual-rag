package com.prj.manualrag.rag.controller;

import com.prj.manualrag.rag.service.WebSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class WebSearchController {
    private final WebSearchService webSearchService;

    @GetMapping
    public String search(@RequestParam String q) {
        return webSearchService.search(q);
    }
}
