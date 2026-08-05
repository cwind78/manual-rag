package com.prj.manualrag.rag.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

public record IntentDecision(
        @JsonDeserialize(using = RouteListDeserializer.class)
        List<String> routes,
        double confidence,
        List<RouteCandidate> candidates
) {
    public record RouteCandidate(String route, double confidence, String reason) {}
}
