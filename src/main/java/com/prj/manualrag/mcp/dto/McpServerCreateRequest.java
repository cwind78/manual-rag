package com.prj.manualrag.mcp.dto;

public record McpServerCreateRequest(
        String name, String description, String endpoint,
        String authType, String accessToken
) {}
