package com.prj.manualrag.chunk;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class Paragraph {
    private final String text;

    /**
     * 원본 Document metadata 유지
     */
    private final Map<String, Object> metadata;

    /**
     * Embedding 캐시
     * 같은 문단을 다시 임베딩하지 않기 위함
     */
    private float[] embedding;


    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}
