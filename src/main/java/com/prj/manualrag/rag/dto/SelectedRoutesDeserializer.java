package com.prj.manualrag.rag.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Accepts the normal string array and also tolerates a single route/object from older clients. */
public class SelectedRoutesDeserializer extends StdDeserializer<List<String>> {
    public SelectedRoutesDeserializer() {
        super(List.class);
    }

    @Override
    public List<String> deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        List<String> routes = new ArrayList<>();

        if (node == null || node.isNull()) return routes;
        if (node.isTextual()) {
            routes.add(node.asText());
            return routes;
        }
        if (node.isArray()) {
            node.forEach(item -> addRoute(item, routes));
            return routes;
        }
        addRoute(node, routes);
        return routes;
    }

    private void addRoute(JsonNode node, List<String> routes) {
        if (node == null || node.isNull()) return;
        if (node.isTextual()) {
            routes.add(node.asText());
        } else if (node.isObject()) {
            JsonNode value = node.get("id");
            if (value == null || !value.isTextual()) value = node.get("route");
            if (value != null && value.isTextual()) routes.add(value.asText());
        }
    }
}
