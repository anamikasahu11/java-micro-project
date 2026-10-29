package com.signaldesk.os;

/** A contiguous interval during which a process (or the CPU idle state) runs. */
public record ExecutionSegment(String processId, int startTime, int endTime) {
    public static final String IDLE_PROCESS_ID = "IDLE";

    public ExecutionSegment {
        if (processId == null || processId.isBlank()) {
            throw new IllegalArgumentException("Segment process id must not be blank");
        }
        if (startTime < 0 || endTime <= startTime) {
            throw new IllegalArgumentException("Segment must have a positive, non-negative interval");
        }
    }

    public boolean isIdle() {
        return IDLE_PROCESS_ID.equals(processId);
    }

    public int duration() {
        return endTime - startTime;
    }

    public static ExecutionSegment idle(int startTime, int endTime) {
        return new ExecutionSegment(IDLE_PROCESS_ID, startTime, endTime);
    }
}
