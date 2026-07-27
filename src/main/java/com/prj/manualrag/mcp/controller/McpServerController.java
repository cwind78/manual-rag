package com.prj.manualrag.mcp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.prj.manualrag.mcp.domain.McpToolEntity;
import com.prj.manualrag.mcp.dto.*;
import com.prj.manualrag.mcp.service.McpServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp/servers")
@RequiredArgsConstructor
public class McpServerController {
    private final McpServerService service;

    @GetMapping
    public List<McpServerResponse> list() {
        return service.findAll().stream().map(McpServerResponse::from).toList();
    }

    @PostMapping
    public McpServerResponse register(@Valid @RequestBody McpServerCreateRequest request) {
        return McpServerResponse.from(service.register(request));
    }

    @PostMapping("/{id}/refresh")
    public List<McpToolResponse> refresh(@PathVariable Long id) {
        return service.refreshTools(id).stream().map(McpToolResponse::from).toList();
    }

    @GetMapping("/{id}/tools")
    public List<McpToolResponse> tools(@PathVariable Long id) {
        return service.tools(id).stream().map(McpToolResponse::from).toList();
    }

    @PatchMapping("/{id}/enabled")
    public McpServerResponse enabled(@PathVariable Long id, @RequestParam boolean value) {
        return McpServerResponse.from(service.toggle(id, value));
    }

    @PostMapping("/{id}/tools/{toolName}/call")
    public JsonNode call(@PathVariable Long id, @PathVariable String toolName,
                         @RequestBody(required = false) Map<String, Object> arguments) {
        return service.callTool(id, toolName, arguments == null ? Map.of() : arguments);
    }
}
