package com.prj.manualrag.document.dto;

public record DocumentUploadResponse(
        Long documentId,
        int chunkCount
) {
}
