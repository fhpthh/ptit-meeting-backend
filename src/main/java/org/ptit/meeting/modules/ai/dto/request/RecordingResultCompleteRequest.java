package org.ptit.meeting.modules.ai.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Only a private manifest reference; never accepts transcript, URLs or a success flag. */
public record RecordingResultCompleteRequest(
    @NotBlank(message = "{validation.ai.recording.required}")
        @Pattern(
            regexp = "[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]",
            message = "{validation.ai.recording.invalid}")
        String bucket,
    @NotBlank(message = "{validation.ai.recording.required}")
        @Size(max = 1024, message = "{validation.ai.recording.invalid}")
        String objectKey,
    @NotNull(message = "{validation.ai.recording.required}")
        @Pattern(regexp = "[a-f0-9]{64}", message = "{validation.ai.recording.invalid}")
        String sha256,
    @NotNull(message = "{validation.ai.recording.required}")
        @Min(value = 1, message = "{validation.ai.recording.invalid}")
        @Max(value = 262144, message = "{validation.ai.recording.invalid}")
        Long sizeBytes) {}
