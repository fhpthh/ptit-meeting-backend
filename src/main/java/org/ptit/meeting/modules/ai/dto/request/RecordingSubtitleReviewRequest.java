package org.ptit.meeting.modules.ai.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class RecordingSubtitleReviewRequest {
  private RecordingSubtitleReviewRequest() {}

  public record Edit(
      @NotBlank String segmentId,
      @Pattern(regexp = "vi|en") @NotNull String language,
      @NotBlank @Size(max = 8000) String text) {}

  public record Save(
      @PositiveOrZero long expectedRevision, @NotNull @Size(max = 2000) List<@Valid Edit> edits) {}

  public record Retranslate(@PositiveOrZero long expectedRevision, @NotBlank String segmentId) {}

  public record Revision(@PositiveOrZero long expectedRevision) {}
}
