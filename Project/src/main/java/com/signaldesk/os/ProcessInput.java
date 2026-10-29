package com.signaldesk.os;

import java.util.Objects;

/** Immutable input describing one process submitted to the CPU scheduler. */
public record ProcessInput(String id, int arrivalTime, int burstTime, int priority) {
    public ProcessInput {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Process id must not be blank");
        }
        if (arrivalTime < 0) {
            throw new IllegalArgumentException("Arrival time must not be negative");
        }
        if (burstTime <= 0) {
            throw new IllegalArgumentException("Burst time must be positive");
        }
    }

    public ProcessInput(String id, int arrivalTime, int burstTime) {
        this(id, arrivalTime, burstTime, 0);
    }
}
