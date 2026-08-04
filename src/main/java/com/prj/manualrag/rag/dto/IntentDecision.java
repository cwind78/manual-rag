package com.prj.manualrag.rag.dto;

import java.util.List;

public record IntentDecision(
        String route,
        double confidence,
        List<RouteCandidate> candidates
) {
    public record RouteCandidate(String route, double confidence, String reason) {}
}
