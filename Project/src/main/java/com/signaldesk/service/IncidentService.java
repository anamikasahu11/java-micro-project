package com.signaldesk.service;

import com.signaldesk.model.AnalyzeRequest;
import com.signaldesk.model.Incident;
import com.signaldesk.model.ServiceStatus;
import com.signaldesk.model.TimelineEvent;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IncidentService {
    private static final Pattern LOG_PATTERN =
            Pattern.compile("^\\[([^]]+)]\\s*\\[([^]]+)]\\s*(.+)$");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AtomicLong nextId = new AtomicLong(1004);
    private final List<Incident> incidents = new ArrayList<>();

    public IncidentService() {
        incidents.add(new Incident(
                1001, "RDS Connection Failure", "HIGH", "INVESTIGATING",
                "RDS database connection pool exhaustion", 87,
                "2026-08-28 10:32:10", List.of(), List.of(), List.of(), List.of(), ""));
        incidents.add(new Incident(
                1002, "API Latency Spike", "MEDIUM", "INVESTIGATING",
                "Database query slowdown causing excessive API latency", 82,
                "2026-08-28 09:15:42", List.of(), List.of(), List.of(), List.of(), ""));
        incidents.add(new Incident(
                1003, "Service Deployment Failure", "HIGH", "RESOLVED",
                "Application configuration and environment variable mismatch", 91,
                "2026-08-27 16:42:18", List.of(), List.of(), List.of(), List.of(), ""));
    }

    public synchronized List<Incident> findAll() {
        return incidents.stream()
                .sorted(Comparator.comparingLong(Incident::id).reversed())
                .toList();
    }

    public synchronized Incident analyze(AnalyzeRequest request) {
        List<TimelineEvent> timeline = parseLogs(request.logs());
        if (timeline.isEmpty()) {
            throw new IllegalArgumentException(
                    "No valid log entries found. Expected [time] [service] message.");
        }

        String combined = timeline.stream()
                .map(TimelineEvent::message)
                .reduce("", (left, right) -> left + " " + right)
                .toLowerCase(Locale.ROOT);

        Analysis analysis = chooseAnalysis(combined, timeline);
        List<String> evidence = timeline.stream()
                .filter(event -> event.message().matches("(?i).*pool|connection|timeout|failed|latency|query|startup|environment.*"))
                .map(event -> "[" + event.service() + "] " + event.message())
                .toList();

        Incident incident = new Incident(
                nextId.getAndIncrement(), analysis.title(), analysis.severity(), "INVESTIGATING",
                analysis.rootCause(), analysis.confidence(), LocalDateTime.now().format(DATE_FORMAT),
                timeline, analysis.services(), evidence.isEmpty() ? timeline.stream()
                        .map(event -> "[" + event.service() + "] " + event.message()).toList() : evidence,
                analysis.recommendations(), request.logs());
        incidents.add(incident);
        return incident;
    }

    public synchronized Incident resolve(long id) {
        for (int i = 0; i < incidents.size(); i++) {
            Incident current = incidents.get(i);
            if (current.id() == id) {
                Incident resolved = new Incident(current.id(), current.title(), current.severity(), "RESOLVED",
                        current.rootCause(), current.confidence(), current.createdAt(), current.timeline(),
                        current.affectedServices(), current.evidence(), current.recommendations(), current.rawLogs());
                incidents.set(i, resolved);
                return resolved;
            }
        }
        throw new IllegalArgumentException("Incident " + id + " was not found.");
    }

    private List<TimelineEvent> parseLogs(String logs) {
        List<TimelineEvent> events = new ArrayList<>();
        for (String line : logs.split("\\R")) {
            Matcher matcher = LOG_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                events.add(new TimelineEvent(matcher.group(1), matcher.group(2), matcher.group(3), line.trim()));
            }
        }
        return events;
    }

    private Analysis chooseAnalysis(String combined, List<TimelineEvent> timeline) {
        if (combined.contains("connection pool exhausted") || combined.contains("pool exhaustion")) {
            return new Analysis("RDS Connection Failure", "HIGH",
                    "RDS database connection pool exhaustion", 87,
                    List.of(new ServiceStatus("RDS", "CRITICAL"),
                            new ServiceStatus("Order Service", "CRITICAL"),
                            new ServiceStatus("API Gateway", "DEGRADED"),
                            new ServiceStatus("Authentication Service", "HEALTHY")),
                    List.of("Increase the RDS database connection pool size.",
                            "Investigate long-running or leaked database connections.",
                            "Review Order Service database connection lifecycle.",
                            "Configure connection pool timeout and retry policies.",
                            "Monitor active database connections and pool utilization."));
        }
        if (combined.matches(".*(environment variable|startup failed|deployment failed|missing configuration).*")) {
            return new Analysis("Service Deployment Failure", "HIGH",
                    "Application configuration and environment variable mismatch", 91,
                    List.of(new ServiceStatus("Order Service", "CRITICAL"),
                            new ServiceStatus("API Gateway", "DEGRADED"),
                            new ServiceStatus("Authentication Service", "HEALTHY")),
                    List.of("Verify deployment environment variables.",
                            "Validate application configuration before deployment.",
                            "Add environment variable validation during startup.",
                            "Improve deployment health checks.",
                            "Use configuration templates to prevent missing variables."));
        }
        return new Analysis("API Latency Spike", "MEDIUM",
                "Database query slowdown causing excessive API latency", 82,
                List.of(new ServiceStatus("API Gateway", "DEGRADED"),
                        new ServiceStatus("Order Service", "DEGRADED"),
                        new ServiceStatus("Database", "CRITICAL"),
                        new ServiceStatus("Authentication Service", "HEALTHY")),
                List.of("Identify slow database queries.", "Review database indexes.",
                        "Check database CPU and memory utilization.", "Review API timeout configuration.",
                        "Enable query performance monitoring."));
    }

    private record Analysis(String title, String severity, String rootCause, int confidence,
                            List<ServiceStatus> services, List<String> recommendations) {
    }
}
