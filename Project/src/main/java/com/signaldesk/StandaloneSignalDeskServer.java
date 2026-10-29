package com.signaldesk;

import com.signaldesk.model.Incident;
import com.signaldesk.model.ServiceStatus;
import com.signaldesk.model.TimelineEvent;
import com.signaldesk.os.CpuScheduler;
import com.signaldesk.os.ExecutionSegment;
import com.signaldesk.os.ProcessInput;
import com.signaldesk.os.ProcessMetrics;
import com.signaldesk.os.SchedulingAlgorithm;
import com.signaldesk.os.SchedulingResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Zero-dependency demo server. Run with the JDK:
 * javac -d out src/main/java/com/signaldesk/model/*.java src/main/java/com/signaldesk/StandaloneSignalDeskServer.java
 * java -cp out com.signaldesk.StandaloneSignalDeskServer
 */
public final class StandaloneSignalDeskServer {
    private static final int PORT = 8080;
    private static final Pattern LOG_PATTERN =
            Pattern.compile("^\\[([^]]+)]\\s*\\[([^]]+)]\\s*(.+)$");
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final List<Incident> incidents = new ArrayList<>();
    private static final AtomicLong nextId = new AtomicLong(1004);

    private StandaloneSignalDeskServer() {
    }

    public static void main(String[] args) throws IOException {
        seedIncidents();
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/incidents", StandaloneSignalDeskServer::handleIncidents);
        server.createContext("/api/os/scheduling", StandaloneSignalDeskServer::handleScheduling);
        server.createContext("/", StandaloneSignalDeskServer::handleStaticFiles);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Signal Desk Java server running at http://localhost:" + PORT);
        System.out.println("Press Ctrl+C to stop.");
    }

    private static void handleScheduling(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if (!"POST".equals(method)) {
                respond(exchange, 405, "{\"error\":\"Only POST is supported\"}");
                return;
            }
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            List<ProcessInput> processes = processInputs(requestBody);
            int quantum = integerValue(requestBody, "timeQuantum", 2);
            CpuScheduler scheduler = new CpuScheduler();
            if ("/api/os/scheduling/run".equals(path)) {
                SchedulingAlgorithm algorithm = algorithmValue(requestBody);
                SchedulingResult result = schedule(scheduler, algorithm, processes, quantum);
                respond(exchange, 200, schedulingJson(result, processes));
                return;
            }
            if ("/api/os/scheduling/compare".equals(path)) {
                StringBuilder results = new StringBuilder("{\"results\":[");
                SchedulingAlgorithm[] algorithms = {
                        SchedulingAlgorithm.FCFS, SchedulingAlgorithm.SJF,
                        SchedulingAlgorithm.PRIORITY, SchedulingAlgorithm.ROUND_ROBIN};
                for (int i = 0; i < algorithms.length; i++) {
                    if (i > 0) results.append(',');
                    results.append(schedulingJson(schedule(scheduler, algorithms[i], processes, quantum), processes));
                }
                respond(exchange, 200, results.append("]}").toString());
                return;
            }
            respond(exchange, 404, "{\"error\":\"Scheduling route not found\"}");
        } catch (IllegalArgumentException error) {
            respond(exchange, 400, "{\"error\":\"" + escape(error.getMessage()) + "\"}");
        } catch (Exception error) {
            respond(exchange, 500, "{\"error\":\"Scheduling server error\"}");
            error.printStackTrace();
        }
    }

    private static SchedulingResult schedule(CpuScheduler scheduler, SchedulingAlgorithm algorithm,
                                             List<ProcessInput> processes, int quantum) {
        return switch (algorithm) {
            case FCFS -> scheduler.fcfs(processes);
            case SJF -> scheduler.sjf(processes);
            case PRIORITY -> scheduler.priority(processes);
            case ROUND_ROBIN -> scheduler.roundRobin(processes, quantum);
        };
    }

    private static SchedulingAlgorithm algorithmValue(String body) {
        String value = stringValue(body, "algorithm");
        try {
            return SchedulingAlgorithm.valueOf(value);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Unknown scheduling algorithm: " + value);
        }
    }

    private static List<ProcessInput> processInputs(String body) {
        int arrayStart = body.indexOf("\"processes\"");
        int start = body.indexOf('[', arrayStart);
        int end = body.indexOf(']', start);
        if (arrayStart < 0 || start < 0 || end < 0) {
            throw new IllegalArgumentException("Request must include a processes array.");
        }
        String array = body.substring(start + 1, end);
        Pattern processPattern = Pattern.compile("\\{\\s*\"pid\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"arrivalTime\"\\s*:\\s*(-?\\d+)\\s*,\\s*\"burstTime\"\\s*:\\s*(-?\\d+)\\s*,\\s*\"priority\"\\s*:\\s*(-?\\d+)\\s*\\}");
        Matcher matcher = processPattern.matcher(array);
        List<ProcessInput> processes = new ArrayList<>();
        while (matcher.find()) {
            processes.add(new ProcessInput(matcher.group(1), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)), Integer.parseInt(matcher.group(4))));
        }
        if (processes.isEmpty()) throw new IllegalArgumentException("At least one process is required.");
        return processes;
    }

    private static int integerValue(String body, String key, int fallback) {
        Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)").matcher(body);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : fallback;
    }

    private static String stringValue(String body, String key) {
        Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(body);
        if (!matcher.find()) throw new IllegalArgumentException("Request must include " + key + ".");
        return matcher.group(1);
    }

    private static String schedulingJson(SchedulingResult result, List<ProcessInput> input) {
        int busy = result.segments().stream().filter(segment -> !segment.isIdle())
                .mapToInt(ExecutionSegment::duration).sum();
        int elapsed = result.segments().isEmpty() ? 0
                : result.segments().get(result.segments().size() - 1).endTime();
        double utilization = elapsed == 0 ? 0 : busy * 100.0 / elapsed;
        StringBuilder json = new StringBuilder("{\"algorithm\":\"").append(result.algorithm())
                .append("\",\"averageWaitingTime\":").append(result.averageWaitingTime())
                .append(",\"averageTurnaroundTime\":").append(result.averageTurnaroundTime())
                .append(",\"averageResponseTime\":").append(result.averageResponseTime())
                .append(",\"totalProcesses\":").append(input.size())
                .append(",\"totalBusyTime\":").append(busy)
                .append(",\"totalElapsedTime\":").append(elapsed)
                .append(",\"cpuUtilization\":").append(utilization)
                .append(",\"segments\":").append(segmentsJson(result.segments()))
                .append(",\"metrics\":").append(metricsJson(result, input)).append('}');
        return json.toString();
    }

    private static String segmentsJson(List<ExecutionSegment> segments) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) json.append(',');
            ExecutionSegment segment = segments.get(i);
            json.append("{\"pid\":\"").append(escape(segment.processId()))
                    .append("\",\"startTime\":").append(segment.startTime())
                    .append(",\"endTime\":").append(segment.endTime())
                    .append(",\"duration\":").append(segment.duration()).append('}');
        }
        return json.append(']').toString();
    }

    private static String metricsJson(SchedulingResult result, List<ProcessInput> input) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < input.size(); i++) {
            if (i > 0) json.append(',');
            ProcessInput process = input.get(i);
            ProcessMetrics metric = result.metrics().get(process.id());
            json.append("{\"pid\":\"").append(escape(process.id()))
                    .append("\",\"arrivalTime\":").append(process.arrivalTime())
                    .append(",\"burstTime\":").append(process.burstTime())
                    .append(",\"priority\":").append(process.priority())
                    .append(",\"completionTime\":").append(metric.completionTime())
                    .append(",\"turnaroundTime\":").append(metric.turnaroundTime())
                    .append(",\"waitingTime\":").append(metric.waitingTime())
                    .append(",\"responseTime\":").append(metric.responseTime()).append('}');
        }
        return json.append(']').toString();
    }

    private static void handleStaticFiles(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            respondText(exchange, 405, "Method Not Allowed", "text/plain; charset=utf-8");
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();
        String relativePath = "/".equals(requestPath) ? "index.html" : requestPath.substring(1);
        Path projectDirectory = Paths.get("").toAbsolutePath().normalize();
        Path file = projectDirectory.resolve(relativePath).normalize();

        if (!file.startsWith(projectDirectory) || !Files.isRegularFile(file)) {
            respondText(exchange, 404, "Not Found", "text/plain; charset=utf-8");
            return;
        }

        String contentType = Files.probeContentType(file);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        respondBytes(exchange, 200, Files.readAllBytes(file), contentType);
    }

    private static void handleIncidents(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method) && "/api/incidents".equals(path)) {
                respond(exchange, 200, incidentsJson());
                return;
            }

            if ("POST".equals(method) && "/api/incidents/analyze".equals(path)) {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String logs = jsonString(requestBody, "logs");
                Incident incident = analyze(logs);
                synchronized (incidents) {
                    incidents.add(incident);
                }
                respond(exchange, 200, incidentJson(incident));
                return;
            }

            if ("PATCH".equals(method) && path.matches("/api/incidents/\\d+/resolve")) {
                long id = Long.parseLong(path.substring("/api/incidents/".length(), path.length() - "/resolve".length()));
                Incident resolved = resolve(id);
                respond(exchange, 200, incidentJson(resolved));
                return;
            }

            respond(exchange, 404, "{\"error\":\"Route not found\"}");
        } catch (IllegalArgumentException error) {
            respond(exchange, 400, "{\"error\":\"" + escape(error.getMessage()) + "\"}");
        } catch (Exception error) {
            respond(exchange, 500, "{\"error\":\"Server error\"}");
            error.printStackTrace();
        }
    }

    private static Incident analyze(String logs) {
        List<TimelineEvent> timeline = parseLogs(logs);
        if (timeline.isEmpty()) {
            throw new IllegalArgumentException("No valid log entries found.");
        }

        String combined = timeline.stream()
                .map(TimelineEvent::message)
                .reduce("", (left, right) -> left + " " + right)
                .toLowerCase(Locale.ROOT);

        String title;
        String severity;
        String cause;
        int confidence;
        List<ServiceStatus> services;
        List<String> recommendations;

        if (combined.contains("connection pool exhausted") || combined.contains("pool exhaustion")) {
            title = "RDS Connection Failure";
            severity = "HIGH";
            cause = "RDS database connection pool exhaustion";
            confidence = 87;
            services = List.of(
                    new ServiceStatus("RDS", "CRITICAL"),
                    new ServiceStatus("Order Service", "CRITICAL"),
                    new ServiceStatus("API Gateway", "DEGRADED"),
                    new ServiceStatus("Authentication Service", "HEALTHY"));
            recommendations = List.of(
                    "Increase the RDS database connection pool size.",
                    "Investigate long-running or leaked database connections.",
                    "Review Order Service database connection lifecycle.",
                    "Configure connection pool timeout and retry policies.",
                    "Monitor active database connections and pool utilization.");
        } else if (combined.matches(".*(environment variable|startup failed|deployment failed|missing configuration).*")) {
            title = "Service Deployment Failure";
            severity = "HIGH";
            cause = "Application configuration and environment variable mismatch";
            confidence = 91;
            services = List.of(
                    new ServiceStatus("Order Service", "CRITICAL"),
                    new ServiceStatus("API Gateway", "DEGRADED"),
                    new ServiceStatus("Authentication Service", "HEALTHY"));
            recommendations = List.of(
                    "Verify deployment environment variables.",
                    "Validate application configuration before deployment.",
                    "Add environment variable validation during startup.",
                    "Improve deployment health checks.",
                    "Use configuration templates to prevent missing variables.");
        } else {
            title = "API Latency Spike";
            severity = "MEDIUM";
            cause = "Database query slowdown causing excessive API latency";
            confidence = 82;
            services = List.of(
                    new ServiceStatus("API Gateway", "DEGRADED"),
                    new ServiceStatus("Order Service", "DEGRADED"),
                    new ServiceStatus("Database", "CRITICAL"),
                    new ServiceStatus("Authentication Service", "HEALTHY"));
            recommendations = List.of(
                    "Identify slow database queries.",
                    "Review database indexes.",
                    "Check database CPU and memory utilization.",
                    "Review API timeout configuration.",
                    "Enable query performance monitoring.");
        }

        List<String> evidence = timeline.stream()
                .map(event -> "[" + event.service() + "] " + event.message())
                .toList();
        return new Incident(nextId.getAndIncrement(), title, severity, "INVESTIGATING", cause,
                confidence, LocalDateTime.now().format(DATE_FORMAT), timeline, services,
                evidence, recommendations, logs);
    }

    private static List<TimelineEvent> parseLogs(String logs) {
        List<TimelineEvent> events = new ArrayList<>();
        for (String line : logs.split("\\R")) {
            Matcher matcher = LOG_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                events.add(new TimelineEvent(matcher.group(1), matcher.group(2),
                        matcher.group(3), line.trim()));
            }
        }
        return events;
    }

    private static Incident resolve(long id) {
        synchronized (incidents) {
            for (int i = 0; i < incidents.size(); i++) {
                Incident current = incidents.get(i);
                if (current.id() == id) {
                    Incident resolved = new Incident(current.id(), current.title(), current.severity(),
                            "RESOLVED", current.rootCause(), current.confidence(), current.createdAt(),
                            current.timeline(), current.affectedServices(), current.evidence(),
                            current.recommendations(), current.rawLogs());
                    incidents.set(i, resolved);
                    return resolved;
                }
            }
        }
        throw new IllegalArgumentException("Incident " + id + " was not found.");
    }

    private static void seedIncidents() {
        incidents.add(new Incident(1001, "RDS Connection Failure", "HIGH", "INVESTIGATING",
                "RDS database connection pool exhaustion", 87, "2026-08-28 10:32:10",
                List.of(), List.of(), List.of(), List.of(), ""));
        incidents.add(new Incident(1002, "API Latency Spike", "MEDIUM", "INVESTIGATING",
                "Database query slowdown causing excessive API latency", 82, "2026-08-28 09:15:42",
                List.of(), List.of(), List.of(), List.of(), ""));
        incidents.add(new Incident(1003, "Service Deployment Failure", "HIGH", "RESOLVED",
                "Application configuration and environment variable mismatch", 91, "2026-08-27 16:42:18",
                List.of(), List.of(), List.of(), List.of(), ""));
    }

    private static String jsonString(String body, String key) {
        String marker = "\"" + key + "\"";
        int keyIndex = body.indexOf(marker);
        int colon = body.indexOf(':', keyIndex + marker.length());
        int start = body.indexOf('"', colon + 1);
        if (keyIndex < 0 || colon < 0 || start < 0) {
            throw new IllegalArgumentException("Request must include logs.");
        }
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = start + 1; i < body.length(); i++) {
            char character = body.charAt(i);
            if (escaped) {
                value.append(character == 'n' ? '\n' : character);
                escaped = false;
            } else if (character == '\\') {
                escaped = true;
            } else if (character == '"') {
                return value.toString();
            } else {
                value.append(character);
            }
        }
        throw new IllegalArgumentException("Invalid logs value.");
    }

    private static String incidentsJson() {
        synchronized (incidents) {
            return incidents.stream().map(StandaloneSignalDeskServer::incidentJson)
                    .reduce("[", (left, right) -> left.equals("[") ? left + right : left + "," + right) + "]";
        }
    }

    private static String incidentJson(Incident incident) {
        return "{\"id\":" + incident.id()
                + ",\"title\":\"" + escape(incident.title())
                + "\",\"severity\":\"" + escape(incident.severity())
                + "\",\"status\":\"" + escape(incident.status())
                + "\",\"rootCause\":\"" + escape(incident.rootCause())
                + "\",\"confidence\":" + incident.confidence()
                + ",\"createdAt\":\"" + escape(incident.createdAt())
                + "\",\"timeline\":" + timelineJson(incident.timeline())
                + ",\"affectedServices\":" + servicesJson(incident.affectedServices())
                + ",\"evidence\":" + stringsJson(incident.evidence())
                + ",\"recommendations\":" + stringsJson(incident.recommendations())
                + ",\"rawLogs\":\"" + escape(incident.rawLogs()) + "\"}";
    }

    private static String timelineJson(List<TimelineEvent> events) {
        return events.stream().map(event -> "{\"time\":\"" + escape(event.time())
                + "\",\"service\":\"" + escape(event.service())
                + "\",\"message\":\"" + escape(event.message())
                + "\",\"raw\":\"" + escape(event.raw()) + "\"}")
                .reduce("[", (left, right) -> left.equals("[") ? left + right : left + "," + right) + "]";
    }

    private static String servicesJson(List<ServiceStatus> services) {
        return services.stream().map(service -> "{\"name\":\"" + escape(service.name())
                + "\",\"status\":\"" + escape(service.status()) + "\"}")
                .reduce("[", (left, right) -> left.equals("[") ? left + right : left + "," + right) + "]";
    }

    private static String stringsJson(List<String> values) {
        return values.stream().map(value -> "\"" + escape(value) + "\"")
                .reduce("[", (left, right) -> left.equals("[") ? left + right : left + "," + right) + "]";
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\")
                .replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        respondBytes(exchange, status, body.getBytes(StandardCharsets.UTF_8),
                "application/json; charset=utf-8");
    }

    private static void respondText(HttpExchange exchange, int status, String body, String contentType)
            throws IOException {
        respondBytes(exchange, status, body.getBytes(StandardCharsets.UTF_8), contentType);
    }

    private static void respondBytes(HttpExchange exchange, int status, byte[] bytes, String contentType)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
