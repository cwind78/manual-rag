package com.prj.manualrag.rag.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/** Accepts both ["DOCUMENT", "WEB"] and the legacy "DOCUMENT|WEB" form. */
public class RouteListDeserializer extends StdDeserializer<List<String>> {
    public RouteListDeserializer() { super(List.class); }

    @Override
    public List<String> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node == null || node.isNull()) return List.of();
        if (node.isArray()) {
            return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                    .filter(JsonNode::isTextual)
                    .map(JsonNode::asText)
                    .flatMap(value -> Arrays.stream(value.split("\\|")))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList();
        }
        if (node.isTextual()) {
            return Arrays.stream(node.asText().split("\\|"))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .toList();
        }
        return List.of();
    }
}
