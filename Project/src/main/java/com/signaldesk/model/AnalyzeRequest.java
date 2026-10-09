package com.signaldesk.model;

import jakarta.validation.constraints.NotBlank;

public record AnalyzeRequest(@NotBlank String logs) {
}
