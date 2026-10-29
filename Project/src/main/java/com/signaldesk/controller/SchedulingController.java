package com.signaldesk.controller;

import com.signaldesk.os.CpuScheduler;
import com.signaldesk.os.ExecutionSegment;
import com.signaldesk.os.ProcessInput;
import com.signaldesk.os.ProcessMetrics;
import com.signaldesk.os.SchedulingAlgorithm;
import com.signaldesk.os.SchedulingResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/os/scheduling")
@CrossOrigin(origins = "*")
public class SchedulingController {
    private final CpuScheduler scheduler = new CpuScheduler();

    @PostMapping("/run")
    public ResponseEntity<?> run(@Valid @RequestBody SchedulingRequest request) {
        SchedulingResult result = schedule(request.algorithm(), request.processes(), request.timeQuantum());
        return ResponseEntity.ok(toResponse(result, request.processes()));
    }

    @PostMapping("/compare")
    public ResponseEntity<?> compare(@Valid @RequestBody ComparisonRequest request) {
        List<ProcessInput> processes = request.processes();
        return ResponseEntity.ok(Map.of("results", List.of(
                toResponse(scheduler.fcfs(processes), processes),
                toResponse(scheduler.sjf(processes), processes),
                toResponse(scheduler.priority(processes), processes),
                toResponse(scheduler.roundRobin(processes, request.timeQuantum()), processes))));
    }

    private SchedulingResult schedule(SchedulingAlgorithm algorithm, List<ProcessInput> processes, int quantum) {
        return switch (algorithm) {
            case FCFS -> scheduler.fcfs(processes);
            case SJF -> scheduler.sjf(processes);
            case PRIORITY -> scheduler.priority(processes);
            case ROUND_ROBIN -> scheduler.roundRobin(processes, quantum);
        };
    }

    private Map<String, Object> toResponse(SchedulingResult result, List<ProcessInput> input) {
        int busy = result.segments().stream().filter(segment -> !segment.isIdle())
                .mapToInt(ExecutionSegment::duration).sum();
        int elapsed = result.segments().isEmpty() ? 0
                : result.segments().get(result.segments().size() - 1).endTime();
        return Map.of(
                "algorithm", result.algorithm(),
                "averageWaitingTime", result.averageWaitingTime(),
                "averageTurnaroundTime", result.averageTurnaroundTime(),
                "averageResponseTime", result.averageResponseTime(),
                "totalProcesses", input.size(),
                "totalBusyTime", busy,
                "totalElapsedTime", elapsed,
                "cpuUtilization", elapsed == 0 ? 0 : busy * 100.0 / elapsed,
                "segments", result.segments(),
                "metrics", input.stream().map(process -> metricResponse(process, result.metrics().get(process.id()))).toList());
    }

    private Map<String, Object> metricResponse(ProcessInput process, ProcessMetrics metric) {
        return Map.of("pid", process.id(), "arrivalTime", process.arrivalTime(), "burstTime", process.burstTime(),
                "priority", process.priority(), "completionTime", metric.completionTime(),
                "turnaroundTime", metric.turnaroundTime(), "waitingTime", metric.waitingTime(),
                "responseTime", metric.responseTime());
    }

    public record SchedulingRequest(
            @NotNull SchedulingAlgorithm algorithm,
            @NotEmpty List<ProcessInput> processes,
            int timeQuantum) {
    }

    public record ComparisonRequest(
            @NotEmpty List<ProcessInput> processes,
            @Positive int timeQuantum) {
    }
}
