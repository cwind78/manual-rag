package com.prj.manualrag.document.service;

import com.prj.manualrag.document.dto.DocumentUploadRequest;
import com.prj.manualrag.document.dto.DocumentUploadResponse;
import com.prj.manualrag.document.entity.DocumentEntity;
import com.prj.manualrag.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
//import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentUploadService {
    private final PdfExtractService pdfExtractService;
    private final DocumentRepository documentRepository;
    private final VectorStore vectorStore;
    private final SemanticChunker semanticChunker;

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
                                .title(request.title())
                                .category(request.category())
                                .metadata(request.metadata())
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
//        TokenTextSplitter splitter =
//                new TokenTextSplitter();
//
//        List<Document> chunks =
//                splitter.apply(
//                        documents
//                );
        //페이지 단위 문서를 문단으로 잘라서 문단간 유사성이 있으면 하나의 청크 유사성이 없으면 그 문단을 하나의 청크로
        //하는 spring.ai.Document를 응답
        List<Document> chunks =
                semanticChunker.chunk(documents);

//        chunks.stream()
//                .map(Document::getText)
//                .filter(text -> text.contains("청정탈취필터"))
//                .forEach(text -> log.info("청크 내용:\n{}", text));
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