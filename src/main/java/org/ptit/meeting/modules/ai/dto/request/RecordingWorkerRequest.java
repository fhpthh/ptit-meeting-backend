package org.ptit.meeting.modules.ai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.ptit.meeting.modules.ai.constant.enums.RecordingTranslationStage;

/** Each authenticated request is bound to its job/attempt by the worker JWT. */
public final class RecordingWorkerRequest {
  private RecordingWorkerRequest() {}

  public record ClaimRequest(
      @NotBlank(message = "{validation.ai.recording.required}")
          @Pattern(
              regexp = "[A-Za-z0-9][A-Za-z0-9._:-]{0,127}",
              message = "{validation.ai.recording.invalid}")
          String requestId) {}

  public record CheckpointRequest(
      @NotBlank(message = "{validation.ai.recording.required}") String objectKey,
      @Pattern(regexp = "[a-f0-9]{64}", message = "{validation.ai.recording.invalid}")
          @NotNull(message = "{validation.ai.recording.required}")
          String sha256,
      @Pattern(regexp = "[a-f0-9]{64}", message = "{validation.ai.recording.invalid}")
          @NotNull(message = "{validation.ai.recording.required}")
          String sourceSha256,
      @NotNull(message = "{validation.ai.recording.required}")
          @PositiveOrZero(message = "{validation.ai.recording.invalid}")
          Long lastCompletedSample) {}

  public record ProgressRequest(
      @NotNull(message = "{validation.ai.recording.required}")
          @Positive(message = "{validation.ai.recording.invalid}")
          Long sequence,
      @NotNull(message = "{validation.ai.recording.required}") RecordingTranslationStage stage,
      @NotNull(message = "{validation.ai.recording.required}")
          @Min(value = 0, message = "{validation.ai.recording.invalid}")
          @Max(value = 99, message = "{validation.ai.recording.invalid}")
          Integer percent,
      @Valid CheckpointRequest checkpoint) {}

  public record FailureRequest(
      @NotNull(message = "{validation.ai.recording.required}")
          @Pattern(regexp = "[A-Z][A-Z0-9_]{0,63}", message = "{validation.ai.recording.invalid}")
          String code,
      @NotNull(message = "{validation.ai.recording.required}") Boolean retryable) {}
}
