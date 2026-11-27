package org.ptit.meeting.modules.ai.dto.internal;

/** Scalar projection: avoids caching an unlocked job entity before acquiring write locks. */
public record RecordingCompletionAuthority(String recordingId, Long requestedByUserId) {}
