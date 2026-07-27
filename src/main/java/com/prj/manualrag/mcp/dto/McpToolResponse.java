package com.prj.manualrag.mcp.dto;

import com.prj.manualrag.mcp.domain.McpToolEntity;

public record McpToolResponse(Long id, String name, String description,
                              String inputSchema, boolean requiresApproval) {
    public static McpToolResponse from(McpToolEntity t) {
        return new McpToolResponse(t.getId(), t.getName(), t.getDescription(),
                t.getInputSchema(), t.isRequiresApproval());
    }
}
