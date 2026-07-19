package com.prj.manualrag.rag.service;

import com.prj.manualrag.rag.dto.SearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DocumentSearchTool {

    private final DocumentSearchService searchService;

    @Tool(description = """
            사용자가 업로드한 문서를 검색한다.
            
            다음 질문은 반드시 이 Tool을 사용한다.
            
            - 설명서
            - 규정
            - 계약
            - PDF
            - 사용자가 업로드한 파일
            - 문서 내용
            
            검색된 문서를 그대로 반환한다.
            """)
    public String search(String question){

        SearchResult result =
                searchService.search(question);

        return result.context();
    }

}