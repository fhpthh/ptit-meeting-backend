package org.ptit.meeting.modules.ai.dto.request;

import jakarta.validation.constraints.Pattern;

public record RecordingTranslationCreateRequest(
    @Pattern(
            regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}",
            message = "{validation.ai.recording.invalid}")
        String expectedRunId) {}
