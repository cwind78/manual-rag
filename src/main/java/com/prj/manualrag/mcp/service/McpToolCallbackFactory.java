package com.prj.manualrag.mcp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prj.manualrag.mcp.client.McpToolCallback;
import com.prj.manualrag.mcp.client.RemoteMcpClient;
import com.prj.manualrag.mcp.domain.McpServerEntity;
import com.prj.manualrag.mcp.domain.McpToolEntity;
import com.prj.manualrag.mcp.repository.McpServerRepository;
import com.prj.manualrag.mcp.repository.McpToolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class McpToolCallbackFactory {
    private final McpServerRepository serverRepository;
    private final McpToolRepository toolRepository;
    private final RemoteMcpClient remoteMcpClient;
    private final ObjectMapper objectMapper;
    private final McpServerService mcpServerService;

    public List<ToolCallback> createActiveCallbacks() {
        List<ToolCallback> callbacks = new ArrayList<>();
        for (McpServerEntity server : serverRepository.findAllByEnabledTrue()) {
            List<McpToolEntity> tools =
                    toolRepository.findAllByServerOrderByNameAsc(server);

            if (tools.isEmpty()) {
                log.info("MCP tools empty; refreshing remote server: server={}, endpoint={}",
                        server.getName(), server.getEndpoint());
                try {
                    tools = mcpServerService.refreshTools(server.getId());
                } catch (Exception e) {
                    log.error("MCP auto refresh failed: server={}", server.getName(), e);
                    continue;
                }
            }

            for (McpToolEntity tool : tools) {
                callbacks.add(new McpToolCallback(
                        server, tool, remoteMcpClient, objectMapper));
            }
        }
        return callbacks;
    }
}
