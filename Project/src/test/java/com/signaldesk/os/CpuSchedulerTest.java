package com.signaldesk.os;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CpuSchedulerTest {
    private final CpuScheduler scheduler = new CpuScheduler();
    private final List<ProcessInput> processes = List.of(
            new ProcessInput("P1", 0, 5, 2),
            new ProcessInput("P2", 1, 3, 1),
            new ProcessInput("P3", 2, 1, 3));

    @Test
    void fcfsCalculatesMetrics() {
        SchedulingResult result = scheduler.fcfs(processes);
        assertEquals(List.of("P1", "P2", "P3"),
                result.segments().stream().map(ExecutionSegment::processId).toList());
        assertEquals(5, result.metrics().get("P1").completionTime());
        assertEquals(7, result.metrics().get("P2").turnaroundTime());
        assertEquals(5, result.metrics().get("P3").waitingTime());
    }

    @Test
    void sjfAndPriorityOnlyUseArrivedProcesses() {
        List<ProcessInput> input = List.of(
                new ProcessInput("Long", 0, 8, 3),
                new ProcessInput("Short", 1, 1, 1),
                new ProcessInput("Priority", 2, 2, 2));
        assertEquals("Long", scheduler.sjf(input).segments().get(0).processId());
        assertEquals("Long", scheduler.priority(input).segments().get(0).processId());
    }

    @Test
    void roundRobinHandlesIdleTimeAndArrivals() {
        SchedulingResult result = scheduler.roundRobin(
                List.of(new ProcessInput("P1", 2, 3, 1), new ProcessInput("P2", 3, 2, 1)), 1);
        assertEquals("IDLE", result.segments().get(0).processId());
        assertEquals(0, result.metrics().get("P1").responseTime());
        assertEquals(5, result.segments().stream().filter(segment -> !segment.isIdle())
                .mapToInt(ExecutionSegment::duration).sum());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class,
                () -> scheduler.fcfs(List.of(new ProcessInput("P1", 0, 1, 1),
                        new ProcessInput("P1", 1, 2, 2))));
        assertThrows(IllegalArgumentException.class,
                () -> scheduler.roundRobin(processes, 0));
        assertThrows(IllegalArgumentException.class, () -> scheduler.fcfs(List.of()));
    }
}
