package org.ptit.meeting.modules.ai.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.integration.ai.RecordingTextTranslationGateway;
import org.ptit.meeting.modules.ai.constant.AiErrorConstants;
import org.ptit.meeting.modules.ai.dto.request.RecordingSubtitleReviewRequest;
import org.ptit.meeting.modules.ai.dto.response.RecordingSubtitleReviewResponse;
import org.ptit.meeting.modules.ai.service.RecordingSubtitleRetranslationService;
import org.ptit.meeting.modules.ai.service.RecordingSubtitleReviewService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecordingSubtitleRetranslationServiceImpl
    implements RecordingSubtitleRetranslationService {
  private final RecordingSubtitleReviewService review;
  private final RecordingTextTranslationGateway gateway;

  @Override
  public RecordingSubtitleReviewResponse translate(
      String id, Long user, long revision, String segmentId) {
    var snapshot = review.get(id, user);
    if (snapshot == null || snapshot.deleted() || snapshot.revision() != revision)
      throw new BusinessException(StatusCode.CONFLICT, AiErrorConstants.RECORDING_CONFLICT);
    String text = null;
    boolean english = false;
    for (var row : snapshot.document().path("segments"))
      if (row.path("segmentId").asText().equals(segmentId)) {
        if (row.path("language").asText().equals("vi")) text = row.path("text").asText();
        if (row.path("language").asText().equals("en")) english = true;
      }
    if (text == null || !english)
      throw BusinessException.invalidInput(AiErrorConstants.RECORDING_INVALID);
    String translated;
    try {
      translated = gateway.translate(text);
    } catch (org.ptit.meeting.integration.ai.AiRuntimeIntegrationException e) {
      throw BusinessException.invalidInput(AiErrorConstants.UNAVAILABLE);
    }
    // HTTP runs outside a database transaction. Save rechecks access and revision under recording
    // lock.
    return review.save(
        id,
        user,
        new RecordingSubtitleReviewRequest.Save(
            revision,
            List.of(new RecordingSubtitleReviewRequest.Edit(segmentId, "en", translated))));
  }
}
