package com.prj.manualrag.rag.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VectorTestService {
    private final VectorStore vectorStore;
    public void testSave(){
        Document document =
                new Document(
                        "냉장고 필터 청소 방법입니다."
                );

        vectorStore.add(
                List.of(document)
        );
    }
}
