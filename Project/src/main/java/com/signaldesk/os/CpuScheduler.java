package com.signaldesk.os;

import java.util.*;

/**
 * Deterministic, non-preemptive CPU scheduling algorithms and preemptive
 * round-robin. Input order is used as the final tie breaker.
 */
public final class CpuScheduler {
    public SchedulingResult scheduleFCFS(List<ProcessInput> processes) {
        return fcfs(processes);
    }

    public SchedulingResult scheduleSJF(List<ProcessInput> processes) {
        return sjf(processes);
    }

    public SchedulingResult schedulePriority(List<ProcessInput> processes) {
        return priority(processes);
    }

    public SchedulingResult scheduleRoundRobin(List<ProcessInput> processes, int quantum) {
        return roundRobin(processes, quantum);
    }

    public SchedulingResult fcfs(List<ProcessInput> processes) {
        return scheduleNonPreemptive(processes, SchedulingAlgorithm.FCFS,
                Comparator.comparingInt(ProcessInput::arrivalTime));
    }

    public SchedulingResult sjf(List<ProcessInput> processes) {
        return scheduleNonPreemptive(processes, SchedulingAlgorithm.SJF,
                Comparator.comparingInt(ProcessInput::burstTime)
                        .thenComparingInt(ProcessInput::arrivalTime));
    }

    public SchedulingResult priority(List<ProcessInput> processes) {
        return scheduleNonPreemptive(processes, SchedulingAlgorithm.PRIORITY,
                Comparator.comparingInt(ProcessInput::priority)
                        .thenComparingInt(ProcessInput::arrivalTime));
    }

    public SchedulingResult roundRobin(List<ProcessInput> processes, int quantum) {
        List<ProcessInput> input = validate(processes);
        if (quantum <= 0) throw new IllegalArgumentException("Quantum must be positive");
        List<ExecutionSegment> segments = new ArrayList<>();
        Map<String, Integer> remaining = new HashMap<>(), firstStart = new HashMap<>(), completion = new HashMap<>();
        for (ProcessInput p : input) remaining.put(p.id(), p.burstTime());
        List<ProcessInput> ordered = new ArrayList<>(input);
        ordered.sort(Comparator.comparingInt(ProcessInput::arrivalTime).thenComparingInt(input::indexOf));
        ArrayDeque<ProcessInput> queue = new ArrayDeque<>();
        int next = 0, time = 0, done = 0;
        while (done < input.size()) {
            if (queue.isEmpty() && next < ordered.size() && time < ordered.get(next).arrivalTime()) {
                int arrival = ordered.get(next).arrivalTime();
                addSegment(segments, ExecutionSegment.idle(time, arrival));
                time = arrival;
            }
            while (next < ordered.size() && ordered.get(next).arrivalTime() <= time) queue.add(ordered.get(next++));
            ProcessInput p = queue.remove();
            firstStart.putIfAbsent(p.id(), time);
            int run = Math.min(quantum, remaining.get(p.id()));
            addSegment(segments, new ExecutionSegment(p.id(), time, time + run));
            time += run;
            remaining.put(p.id(), remaining.get(p.id()) - run);
            // Arrivals at the boundary enter before the preempted process is requeued.
            while (next < ordered.size() && ordered.get(next).arrivalTime() <= time) queue.add(ordered.get(next++));
            if (remaining.get(p.id()) > 0) queue.add(p);
            else { completion.put(p.id(), time); done++; }
        }
        return result(SchedulingAlgorithm.ROUND_ROBIN, input, segments, firstStart, completion);
    }

    private SchedulingResult scheduleNonPreemptive(List<ProcessInput> processes,
                                                    SchedulingAlgorithm algorithm,
                                                    Comparator<ProcessInput> readyOrder) {
        List<ProcessInput> input = validate(processes);
        List<ProcessInput> pending = new ArrayList<>(input);
        Map<String, Integer> firstStart = new HashMap<>(), completion = new HashMap<>();
        List<ExecutionSegment> segments = new ArrayList<>();
        int time = 0;
        while (!pending.isEmpty()) {
            int currentTime = time;
            List<ProcessInput> ready = pending.stream().filter(p -> p.arrivalTime() <= currentTime).toList();
            if (ready.isEmpty()) {
                int next = pending.stream().mapToInt(ProcessInput::arrivalTime).min().orElse(time);
                addSegment(segments, ExecutionSegment.idle(time, next));
                time = next;
                continue;
            }
            ProcessInput chosen = ready.stream().min(readyOrder.thenComparingInt(input::indexOf)).orElseThrow();
            pending.remove(chosen);
            firstStart.put(chosen.id(), time);
            time += chosen.burstTime();
            completion.put(chosen.id(), time);
            addSegment(segments, new ExecutionSegment(chosen.id(), time - chosen.burstTime(), time));
        }
        return result(algorithm, input, segments, firstStart, completion);
    }

    private static SchedulingResult result(SchedulingAlgorithm algorithm, List<ProcessInput> input,
                                           List<ExecutionSegment> segments, Map<String, Integer> first,
                                           Map<String, Integer> completion) {
        Map<String, ProcessMetrics> metrics = new LinkedHashMap<>();
        for (ProcessInput p : input) {
            int complete = completion.get(p.id());
            int turnaround = complete - p.arrivalTime();
            metrics.put(p.id(), new ProcessMetrics(p.id(), complete, turnaround,
                    turnaround - p.burstTime(), first.get(p.id()) - p.arrivalTime()));
        }
        return new SchedulingResult(algorithm, segments, metrics);
    }

    private static List<ProcessInput> validate(List<ProcessInput> processes) {
        if (processes == null || processes.isEmpty()) throw new IllegalArgumentException("At least one process is required");
        List<ProcessInput> copy = List.copyOf(processes);
        Set<String> ids = new HashSet<>();
        for (ProcessInput p : copy) {
            if (p == null) throw new IllegalArgumentException("Process must not be null");
            if (!ids.add(p.id())) throw new IllegalArgumentException("Duplicate process id: " + p.id());
        }
        return copy;
    }

    private static void addSegment(List<ExecutionSegment> segments, ExecutionSegment segment) {
        if (!segments.isEmpty()) {
            ExecutionSegment last = segments.get(segments.size() - 1);
            if (last.processId().equals(segment.processId()) && last.endTime() == segment.startTime()) {
                segments.set(segments.size() - 1, new ExecutionSegment(last.processId(), last.startTime(), segment.endTime()));
                return;
            }
        }
        segments.add(segment);
    }
}
