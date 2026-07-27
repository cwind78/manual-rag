package com.prj.manualrag.mcp.dto;

import com.prj.manualrag.mcp.domain.McpServerEntity;

public record McpServerResponse(
        Long id, String name, String description, String endpoint,
        String authType, boolean enabled
) {
    public static McpServerResponse from(McpServerEntity s) {
        return new McpServerResponse(s.getId(), s.getName(), s.getDescription(),
                s.getEndpoint(), s.getAuthType(), s.isEnabled());
    }
}
