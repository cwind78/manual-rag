package com.prj.manualrag.capability;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CapabilitySearchService {
    private final VectorStore vectorStore;

    public void register(String type, String sourceId, String name, String summary) {
        vectorStore.add(List.of(new Document(summary, Map.of(
                "capability", true,
                "capabilityType", type,
                "capabilitySourceId", sourceId,
                "capabilityName", name
        ))));
        log.info("Capability registered: type={}, sourceId={}, name={}", type, sourceId, name);
    }

    public List<Document> search(String question, int topK) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(question).topK(topK).similarityThreshold(0.0)
                .filterExpression("capability == true").build());
    }
}
