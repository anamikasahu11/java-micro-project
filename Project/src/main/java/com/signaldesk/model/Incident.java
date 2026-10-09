package com.signaldesk.model;

import java.util.List;

public record Incident(
        long id,
        String title,
        String severity,
        String status,
        String rootCause,
        int confidence,
        String createdAt,
        List<TimelineEvent> timeline,
        List<ServiceStatus> affectedServices,
        List<String> evidence,
        List<String> recommendations,
        String rawLogs) {
}
