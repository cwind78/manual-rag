package com.prj.manualrag.document.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class PdfExtractService {
    public List<Document> extract(MultipartFile file, Long documentId) {
        List<Document> documents = new ArrayList<>();
        try (PDDocument pdf =
                     Loader.loadPDF(file.getBytes())) {


            PDFTextStripper stripper =
                    new PDFTextStripper();


            int pageCount =
                    ((org.apache.pdfbox.pdmodel.PDDocument) pdf).getNumberOfPages();


            for (int page = 1; page <= pageCount; page++) {


                stripper.setStartPage(page);
                stripper.setEndPage(page);


                String text =
                        stripper.getText(pdf).trim();


                // 빈 페이지 제외
                if (text.isBlank()) {
                    continue;
                }


                Document document =
                        new Document(text);


                document.getMetadata()
                        .put(
                                "documentId",
                                documentId
                        );


                document.getMetadata()
                        .put(
                                "pageNumber",
                                page
                        );


                document.getMetadata()
                        .put(
                                "fileName",
                                file.getOriginalFilename()
                        );


                documents.add(document);
            }


        } catch (Exception e) {

            throw new RuntimeException(
                    "PDF extraction failed",
                    e
            );
        }


        return documents;
    }
}
