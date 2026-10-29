package com.signaldesk.os;

/** Completion and latency measurements for one process. */
public record ProcessMetrics(
        String processId,
        int completionTime,
        int turnaroundTime,
        int waitingTime,
        int responseTime) {
    public ProcessMetrics {
        if (processId == null || processId.isBlank()) {
            throw new IllegalArgumentException("Process id must not be blank");
        }
        if (completionTime < 0 || turnaroundTime < 0 || waitingTime < 0 || responseTime < 0) {
            throw new IllegalArgumentException("Metrics must not be negative");
        }
    }
}
