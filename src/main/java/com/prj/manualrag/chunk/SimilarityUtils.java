package com.prj.manualrag.chunk;


import org.springframework.stereotype.Component;

@Component
public class SimilarityUtils {


    /**
     * Cosine Similarity 계산
     *
     * 결과:
     * 1.0  -> 동일
     * 0.8  -> 유사
     * 0.0  -> 관련 없음
     */
    public double cosineSimilarity(
            float[] a,
            float[] b
    ) {

        if (a == null || b == null) {
            return 0.0;
        }


        if (a.length != b.length) {
            throw new IllegalArgumentException(
                    "Embedding dimension mismatch"
            );
        }


        double dotProduct = 0.0;

        double normA = 0.0;

        double normB = 0.0;


        for (int i = 0; i < a.length; i++) {

            dotProduct += a[i] * b[i];

            normA += a[i] * a[i];

            normB += b[i] * b[i];
        }


        if (normA == 0 || normB == 0) {
            return 0.0;
        }


        return dotProduct /
                (Math.sqrt(normA)
                        *
                        Math.sqrt(normB));
    }

}
