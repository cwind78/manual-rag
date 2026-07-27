package com.prj.manualrag.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prj.manualrag.mcp.domain.McpServerEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class RemoteMcpClient {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Map<Long, String> sessionIds = new ConcurrentHashMap<>();

    public JsonNode initialize(McpServerEntity server) {
        log.info("MCP initialize start: server={}, endpoint={}",
                server.getName(), server.getEndpoint());
        JsonNode result = call(server, "initialize", Map.of(
                "protocolVersion", "2025-06-18",
                "capabilities", Map.of(),
                "clientInfo", Map.of("name", "manual-rag", "version", "0.1.0")
        ));
        notify(server, "notifications/initialized", Map.of());
        log.info("MCP initialize complete: server={}, result={}",
                server.getName(), result);
        return result;
    }

    public JsonNode listTools(McpServerEntity server) {
        log.info("MCP tools/list start: server={}, endpoint={}, sessionId={}",
                server.getName(), server.getEndpoint(), sessionIds.get(server.getId()));
        JsonNode result = call(server, "tools/list", Map.of());
        log.info("MCP tools/list result: server={}, result={}", server.getName(), result);
        return result;
    }

    public JsonNode callTool(McpServerEntity server, String name, Map<String, Object> arguments) {
        return call(server, "tools/call", Map.of("name", name, "arguments", arguments));
    }

    private void notify(McpServerEntity server, String method, Map<String, Object> params) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("method", method);
            request.put("params", params);
            log.info("MCP JSON-RPC request: server={}, method={}, request={}",
                    server.getName(), method, request);
            webClient.post().uri(server.getEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        if ("BEARER".equalsIgnoreCase(server.getAuthType())
                                && server.getAccessToken() != null) {
                            headers.setBearerAuth(server.getAccessToken());
                        }
                        headers.set("MCP-Protocol-Version", "2025-06-18");
                        String sessionId = sessionIds.get(server.getId());
                        if (sessionId != null) headers.set("Mcp-Session-Id", sessionId);
                    })
                    .bodyValue(request).retrieve().toBodilessEntity().block();
        } catch (Exception e) {
            throw new IllegalStateException("MCP initialized notification failed", e);
        }
    }

    private JsonNode call(McpServerEntity server, String method, Map<String, Object> params) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("id", System.nanoTime());
            request.put("method", method);
            request.put("params", params);

            WebClient.RequestHeadersSpec<?> requestSpec = webClient.post()
                    .uri(server.getEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                    .headers(headers -> {
                        if ("BEARER".equalsIgnoreCase(server.getAuthType())
                                && server.getAccessToken() != null) {
                            headers.setBearerAuth(server.getAccessToken());
                        }
                        headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");
                        headers.set("MCP-Protocol-Version", "2025-06-18");
                        String sessionId = sessionIds.get(server.getId());
                        if (sessionId != null) headers.set("Mcp-Session-Id", sessionId);
                    })
                    .bodyValue(request);

            ResponseEntity<String> entity = requestSpec.exchangeToMono(response ->
                    response.toEntity(String.class)
            ).block();
            String sessionId = entity.getHeaders().getFirst("Mcp-Session-Id");
            if (sessionId != null && !sessionId.isBlank()) {
                sessionIds.put(server.getId(), sessionId);
            }
            String body = entity.getBody();
            log.info("MCP response: server={}, method={}, status={}, sessionId={}, body={}",
                    server.getName(), method, entity.getStatusCode(), sessionId, body);
            JsonNode response = parseResponse(body);
            if (response.has("error")) {
                throw new IllegalStateException("MCP error: " + response.get("error"));
            }
            return response.path("result");
        } catch (Exception e) {
            log.error("MCP call failed: server={}, method={}, endpoint={}",
                    server.getName(), method, server.getEndpoint(), e);
            throw new IllegalStateException("MCP request failed: " + server.getEndpoint(), e);
        }
    }

    /**
     * Streamable HTTP MCP 서버는 JSON 또는 SSE(data: JSON)로 응답할 수 있다.
     */
    private JsonNode parseResponse(String body) throws Exception {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }

        String trimmed = body.trim();
        if (trimmed.startsWith("{")) {
            return objectMapper.readTree(trimmed);
        }

        StringBuilder data = new StringBuilder();
        for (String line : body.split("\\r?\\n")) {
            if (line.startsWith("data:")) {
                if (data.length() > 0) data.append('\n');
                data.append(line.substring(5).stripLeading());
            }
        }

        if (data.isEmpty()) {
            throw new IllegalStateException("MCP 응답에서 JSON data를 찾을 수 없습니다: " + body);
        }

        return objectMapper.readTree(data.toString());
    }
}
