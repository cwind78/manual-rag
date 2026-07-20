package com.prj.manualrag.document.service;

import com.prj.manualrag.chunk.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SemanticChunker {
    private static final double SIMILARITY_THRESHOLD = 0.83;

    /**
     * 너무 큰 Chunk 방지
     *
     * 문자 기준
     * (추후 Token 기준 변경 가능)
     */
    private static final int MAX_CHUNK_SIZE = 2500;
    private final ParagraphExtractor paragraphExtractor;
    private final HeadingDetector headingDetector;
    private final SimilarityUtils similarityUtils;
    private final EmbeddingModel embeddingModel;

    public List<Document> chunk(List<Document> documents) {
        //개행으로 나누어 문단별로 paragraph을 생성하고 list에 담아 응답
        List<Paragraph> paragraphs = paragraphExtractor.extract(documents);
        List<Document> result = new ArrayList<>();

        if (paragraphs.isEmpty()) {
            return result;
        }

        ChunkCandidate current = null;
        float[] currentEmbedding = null;

        for (Paragraph paragraph : paragraphs) {
            /*
             * 첫 Chunk 시작
             */
            if (current == null) {
                //current.getText:content(StringBuilder)에 값이 있으면 개행 추가하고 파라미터의 텍스트를 추가하여 응답
                current = new ChunkCandidate(paragraph);
                currentEmbedding = embedding(current.getText());
                continue;
            }

            /*
             * 제목이면 강제 분리
             * 첫 청크 이후에 제목 패턴이 나오면 spring.ai.Document에 담고 current를 새로 만듬
             * 실제 문서에서 보면 헤딩 패턴에 해당 하지 않는 경우인데 제목인 경우가 많아서 이거 제거
             */
//            if (headingDetector.isHeading(paragraph.getText())) {
//                result.add(toDocument(current));
//                current = new ChunkCandidate(paragraph);
//                currentEmbedding =embedding(current.getText());
//                continue;
//            }

//            String mergedText = current.getText()
//                            + "\n\n"
//                            + paragraph.getText();

            /*
             * 최대 크기 초과
             */
//            if (mergedText.length() > MAX_CHUNK_SIZE) {
//                result.add(toDocument(current));
//
//                current = new ChunkCandidate(paragraph);
//                currentEmbedding = embedding(current.getText());
//                continue;
//            }

            float[] paragraphEmbedding = embedding(paragraph.getText());
            double similarity =similarityUtils.cosineSimilarity(currentEmbedding, paragraphEmbedding);

            /*
             * 의미적으로 가까우면 합침
             */
            if (similarity >= SIMILARITY_THRESHOLD) {
                current.addParagraph(paragraph);
                currentEmbedding = embedding(current.getText());
            } else {
                result.add(toDocument(current));
                current = new ChunkCandidate(paragraph);
                currentEmbedding = embedding(current.getText());
            }
        }

        if (current != null) {
            result.add(toDocument(current));
        }

        return result;
    }

    private float[] embedding(String text) {
        return embeddingModel.embed(text);
    }

    private Document toDocument(ChunkCandidate chunk) {
        Map<String,Object> metadata = chunk.getMetadata();
        return new Document(chunk.getText(),metadata);
    }
}