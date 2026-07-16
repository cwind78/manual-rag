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



    public List<Document> chunk(
            List<Document> documents
    ) {


        List<Paragraph> paragraphs =
                paragraphExtractor.extract(
                        documents
                );


        List<Document> result =
                new ArrayList<>();


        if (paragraphs.isEmpty()) {
            return result;
        }



        ChunkCandidate current =
                null;


        float[] currentEmbedding =
                null;



        for (Paragraph paragraph : paragraphs) {



            /*
             * 첫 Chunk 시작
             */
            if (current == null) {

                current =
                        new ChunkCandidate(
                                paragraph
                        );

                currentEmbedding =
                        embedding(
                                current.getText()
                        );

                continue;
            }



            /*
             * 제목이면 강제 분리
             */
            if (headingDetector.isHeading(
                    paragraph.getText()
            )) {


                result.add(
                        toDocument(current)
                );


                current =
                        new ChunkCandidate(
                                paragraph
                        );


                currentEmbedding =
                        embedding(
                                current.getText()
                        );


                continue;
            }



            String mergedText =
                    current.getText()
                            + "\n\n"
                            + paragraph.getText();



            /*
             * 최대 크기 초과
             */
            if (mergedText.length()
                    > MAX_CHUNK_SIZE) {


                result.add(
                        toDocument(current)
                );


                current =
                        new ChunkCandidate(
                                paragraph
                        );


                currentEmbedding =
                        embedding(
                                current.getText()
                        );


                continue;
            }



            float[] paragraphEmbedding =
                    embedding(
                            paragraph.getText()
                    );


            double similarity =
                    similarityUtils.cosineSimilarity(
                            currentEmbedding,
                            paragraphEmbedding
                    );



            /*
             * 의미적으로 가까우면 합침
             */
            if (similarity >= SIMILARITY_THRESHOLD) {


                current.addParagraph(
                        paragraph
                );


                currentEmbedding =
                        embedding(
                                current.getText()
                        );


            } else {


                result.add(
                        toDocument(current)
                );


                current =
                        new ChunkCandidate(
                                paragraph
                        );


                currentEmbedding =
                        embedding(
                                current.getText()
                        );
            }

        }



        if (current != null) {

            result.add(
                    toDocument(current)
            );
        }



        return result;
    }





    private float[] embedding(
            String text
    ) {

        return embeddingModel.embed(
                text
        );
    }





    private Document toDocument(
            ChunkCandidate chunk
    ) {

        Map<String,Object> metadata =
                chunk.getMetadata();


        return new Document(
                chunk.getText(),
                metadata
        );
    }

}