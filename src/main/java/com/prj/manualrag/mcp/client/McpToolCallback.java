package com.prj.manualrag.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prj.manualrag.mcp.domain.McpServerEntity;
import com.prj.manualrag.mcp.domain.McpToolEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RequiredArgsConstructor
@Slf4j
public class McpToolCallback implements ToolCallback {
    private final McpServerEntity server;
    private final McpToolEntity tool;
    private final RemoteMcpClient client;
    private final ObjectMapper objectMapper;

    @Override
    public ToolDefinition getToolDefinition() {
        String schema = normalizedInputSchema();
        log.info("MCP tool schema: tool={}, rawSchema={}, normalizedSchema={}",
                toolName(), tool.getInputSchema(), schema);
        return ToolDefinition.builder()
                .name(toolName())
                .description(tool.getDescription() == null ? tool.getName() : tool.getDescription())
                .inputSchema(schema)
                .build();
    }

    private String normalizedInputSchema() {
        try {
            JsonNode schema = objectMapper.readTree(tool.getInputSchema());
            if (schema != null && schema.isObject()) {
                return schema.toString();
            }

            ObjectNode fallback = objectMapper.createObjectNode();
            fallback.put("type", "object");
            fallback.set("properties", objectMapper.createObjectNode());
            log.warn("Invalid MCP input schema; using empty object schema: tool={}, value={}",
                    toolName(), tool.getInputSchema());
            return fallback.toString();
        } catch (Exception e) {
            log.warn("Unreadable MCP input schema; using empty object schema: tool={}, value={}",
                    toolName(), tool.getInputSchema(), e);
            return "{\"type\":\"object\",\"properties\":{}}";
        }
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return ToolMetadata.builder().build();
    }

    @Override
    public String call(String toolInput) {
        try {
            log.info("MCP tool input: tool={}, rawInput={}", toolName(), toolInput);
            JsonNode input = objectMapper.readTree(toolInput);
            Map<String, Object> arguments = normalizeArguments(input);
            log.info("MCP tool arguments: tool={}, arguments={}", toolName(), arguments);
            JsonNode result = client.callTool(server, tool.getName(), arguments);
            return result.toString();
        } catch (Exception e) {
            log.error("MCP 도구 호출 실패: tool={}, input={}", toolName(), toolInput, e);
            // 도구 예외를 그대로 던지지 않고 모델이 재시도하거나 설명할 수 있도록 결과로 반환한다.
            return "{\"error\":\"MCP 도구 호출 실패: "
                    + escape(e.getMessage()) + "\"}";
        }
    }

    private Map<String, Object> normalizeArguments(JsonNode input) throws Exception {
        if (input != null && input.isObject()) {
            return objectMapper.convertValue(input, Map.class);
        }

        JsonNode schema = objectMapper.readTree(normalizedInputSchema());
        JsonNode properties = schema.path("properties");
        if (properties.isObject() && properties.size() == 1) {
            Iterator<String> names = properties.fieldNames();
            String propertyName = names.next();
            Map<String, Object> arguments = new LinkedHashMap<>();
            arguments.put(propertyName, objectMapper.convertValue(input, Object.class));
            log.warn("Primitive MCP input normalized: tool={}, property={}",
                    toolName(), propertyName);
            return arguments;
        }

        if (properties.isObject() && properties.isEmpty()
                && (input == null || input.isNull() || input.isNumber())) {
            return new LinkedHashMap<>();
        }

        throw new IllegalArgumentException(
                "MCP 도구 입력은 객체여야 합니다. input=" + input);
    }

    private String escape(String value) {
        if (value == null) return "unknown";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return call(toolInput);
    }

    private String toolName() {
        return server.getName().replaceAll("[^a-zA-Z0-9_]", "_")
                + "__" + tool.getName().replaceAll("[^a-zA-Z0-9_]", "_");
    }
}
