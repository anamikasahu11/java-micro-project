package com.signaldesk.model;

public record TimelineEvent(String time, String service, String message, String raw) {
}
