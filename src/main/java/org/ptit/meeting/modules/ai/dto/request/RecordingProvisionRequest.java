package org.ptit.meeting.modules.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record RecordingProvisionRequest(
    @NotBlank(message = "{validation.ai.recording.required}")
        @Pattern(
            regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,35}",
            message = "{validation.ai.recording.invalid}")
        String jobId,
    @NotBlank(message = "{validation.ai.recording.required}")
        @Pattern(
            regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,35}",
            message = "{validation.ai.recording.invalid}")
        String eventId,
    @NotNull(message = "{validation.ai.recording.required}")
        @Positive(message = "{validation.ai.recording.invalid}")
        Long dispatchVersion) {}
