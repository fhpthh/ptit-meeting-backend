package org.ptit.meeting.integration.ai.dto;

public record AiRuntimeView(
    int schemaVersion,
    String runtimeSessionId,
    long enableEpoch,
    String state,
    boolean ready,
    int activeTracks,
    int taskFailures
) {
}
