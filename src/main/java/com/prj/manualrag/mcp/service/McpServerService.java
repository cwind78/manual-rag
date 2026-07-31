package com.prj.manualrag.mcp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prj.manualrag.mcp.client.RemoteMcpClient;
import com.prj.manualrag.mcp.domain.McpServerEntity;
import com.prj.manualrag.mcp.domain.McpToolEntity;
import com.prj.manualrag.mcp.dto.McpServerCreateRequest;
import com.prj.manualrag.mcp.repository.McpServerRepository;
import com.prj.manualrag.mcp.repository.McpToolRepository;
import com.prj.manualrag.capability.CapabilitySearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class McpServerService {
    private final McpServerRepository serverRepository;
    private final McpToolRepository toolRepository;
    private final RemoteMcpClient client;
    private final ObjectMapper objectMapper;
    private final CapabilitySearchService capabilitySearchService;

    public List<McpServerEntity> findAll() { return serverRepository.findAllByOrderByNameAsc(); }

    @Transactional
    public McpServerEntity register(McpServerCreateRequest request) {
        if (request.name() == null || request.name().isBlank()
                || request.endpoint() == null || request.endpoint().isBlank()) {
            throw new IllegalArgumentException("MCP 이름과 endpoint는 필수입니다.");
        }
        McpServerEntity server = serverRepository.save(McpServerEntity.builder()
                .name(request.name()).description(request.description())
                .endpoint(request.endpoint()).authType(request.authType() == null ? "NONE" : request.authType())
                .accessToken(request.accessToken()).enabled(true).build());
        refreshTools(server.getId());
        return serverRepository.findById(server.getId()).orElseThrow();
    }

    @Transactional
    public List<McpToolEntity> refreshTools(Long id) {
        McpServerEntity server = get(id);
        log.info("MCP refresh start: id={}, server={}, endpoint={}",
                id, server.getName(), server.getEndpoint());
        client.initialize(server);
        log.info("MCP refresh initialize complete: server={}", server.getName());
        JsonNode result = client.listTools(server);
        log.info("MCP refresh tools/list complete: server={}, result={}",
                server.getName(), result);
        JsonNode tools = result.isArray() ? result : result.path("tools");
        if (!tools.isArray()) {
            throw new IllegalStateException(
                    "MCP tools/list 응답에 tools 배열이 없습니다: " + result);
        }
        toolRepository.deleteAllByServer(server);
        log.info("MCP tools to save: server={}, count={}", server.getName(), tools.size());
        for (JsonNode tool : tools) {
            try {
                JsonNode inputSchema = tool.path("inputSchema");
                log.info("MCP raw tool: name={}, inputSchema={}",
                        tool.path("name").asText(), inputSchema);

                McpToolEntity savedTool = toolRepository.save(McpToolEntity.builder().server(server)
                        .name(tool.path("name").asText())
                        .description(tool.path("description").asText(""))
                        .inputSchema(objectMapper.writeValueAsString(inputSchema))
                        .requiresApproval(false).build());
                capabilitySearchService.register(
                        "MCP_TOOL",
                        server.getId() + ":" + savedTool.getName(),
                        server.getName() + " / " + savedTool.getName(),
                        server.getName() + " " + savedTool.getName() + " "
                                + savedTool.getDescription()
                );
            } catch (Exception e) { throw new IllegalStateException("MCP 도구 저장 실패", e); }
        }
        return toolRepository.findAllByServerOrderByNameAsc(server);
    }

    public McpServerEntity toggle(Long id, boolean enabled) {
        McpServerEntity server = get(id); server.setEnabled(enabled); return serverRepository.save(server);
    }

    public McpServerEntity get(Long id) { return serverRepository.findById(id).orElseThrow(); }
    public List<McpToolEntity> tools(Long id) { return toolRepository.findAllByServerOrderByNameAsc(get(id)); }

    public JsonNode callTool(Long serverId, String name, Map<String, Object> arguments) {
        return client.callTool(get(serverId), name, arguments);
    }
}
