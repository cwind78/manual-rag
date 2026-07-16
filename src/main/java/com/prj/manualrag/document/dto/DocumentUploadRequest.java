package com.prj.manualrag.document.dto;

import java.util.Map;

public record DocumentUploadRequest(String title,
                                    String category,
                                    Map<String, Object> metadata) {
}
