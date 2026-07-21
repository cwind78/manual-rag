package com.prj.manualrag.document.controller;

import com.prj.manualrag.common.dto.ApiResponse;
import com.prj.manualrag.document.dto.DocumentUploadRequest;
import com.prj.manualrag.document.dto.DocumentUploadResponse;
import com.prj.manualrag.document.service.DocumentUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentUploadService documentUploadService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestPart("request") DocumentUploadRequest request
    ){

        return ResponseEntity.ok(ApiResponse.success(
                documentUploadService.upload(file, request))
        );
    }
}
