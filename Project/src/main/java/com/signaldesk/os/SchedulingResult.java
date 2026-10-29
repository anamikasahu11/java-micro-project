package com.signaldesk.os;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;

/** Complete, immutable output of a scheduling run. */
public record SchedulingResult(
        SchedulingAlgorithm algorithm,
        List<ExecutionSegment> segments,
        Map<String, ProcessMetrics> metrics) {
    public SchedulingResult {
        if (algorithm == null) throw new IllegalArgumentException("Algorithm is required");
        segments = List.copyOf(segments);
        metrics = Collections.unmodifiableMap(new LinkedHashMap<>(metrics));
    }

    public double averageWaitingTime() {
        return metrics.values().stream().mapToInt(ProcessMetrics::waitingTime).average().orElse(0);
    }

    public double averageTurnaroundTime() {
        return metrics.values().stream().mapToInt(ProcessMetrics::turnaroundTime).average().orElse(0);
    }

    public double averageResponseTime() {
        return metrics.values().stream().mapToInt(ProcessMetrics::responseTime).average().orElse(0);
    }
}
