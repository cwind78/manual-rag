package com.prj.manualrag.document.service;

import com.prj.manualrag.document.dto.DocumentUploadRequest;
import com.prj.manualrag.document.dto.DocumentUploadResponse;
import com.prj.manualrag.document.entity.DocumentEntity;
import com.prj.manualrag.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentUploadService {
    private final PdfExtractService pdfExtractService;
    private final DocumentRepository documentRepository;
    private final VectorStore vectorStore;

    public DocumentUploadResponse upload(
            MultipartFile file, DocumentUploadRequest request
    ) {
        /*
         * 1. 문서 메타데이터 저장
         */
        DocumentEntity document =
                documentRepository.save(
                        DocumentEntity.builder()
                                .fileName(
                                        file.getOriginalFilename()
                                )
                                .productName(request.productName())
                                .manufacturer(request.manufacturer())
                                .modelName(request.modelName())
                                .build()
                );

        /*
         * 2. PDF 페이지별 Document 생성
         */
        List<Document> documents =
                pdfExtractService.extract(
                        file,
                        document.getId()
                );

        /*
         * 3. Chunk 분할
         */
        TokenTextSplitter splitter =
                new TokenTextSplitter();

        List<Document> chunks =
                splitter.apply(
                        documents
                );

        /*
         * 4. Embedding 생성 + Vector 저장
         */
        vectorStore.add(
                chunks
        );

        return new DocumentUploadResponse(
                document.getId(),
                chunks.size()
        );
    }
}