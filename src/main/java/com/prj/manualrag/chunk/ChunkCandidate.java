package com.prj.manualrag.chunk;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class ChunkCandidate {
    private final List<Paragraph> paragraphs = new ArrayList<>();
    private final StringBuilder content = new StringBuilder();
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 현재 Chunk embedding
     */
    private float[] embedding;

    public ChunkCandidate(Paragraph paragraph) {
        addParagraph(paragraph);
    }



    /**
     * 문단 추가
     */
    public void addParagraph(
            Paragraph paragraph
    ) {

        if (!paragraphs.isEmpty()) {

            content.append("\n\n");
        }


        content.append(
                paragraph.getText()
        );


        paragraphs.add(paragraph);


        if (metadata.isEmpty()) {

            metadata =
                    new HashMap<>(
                            paragraph.getMetadata()
                    );
        }
    }



    public String getText() {

        return content.toString();
    }



    public int length() {

        return content.length();
    }



    public void updateEmbedding(
            float[] embedding
    ) {

        this.embedding = embedding;
    }



    public boolean hasEmbedding() {

        return embedding != null;
    }


}
