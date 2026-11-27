package org.ptit.meeting.modules.ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.ai.dto.request.RecordingSubtitleReviewRequest.Revision;
import org.ptit.meeting.modules.ai.dto.request.RecordingSubtitleReviewRequest.Save;
import org.ptit.meeting.modules.ai.dto.response.RecordingSubtitleReviewResponse;
import org.ptit.meeting.modules.ai.service.RecordingSubtitleReviewService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meeting-recordings/{recordingId}/subtitles")
public class RecordingSubtitleReviewController {
  private final RecordingSubtitleReviewService service;
  private final org.ptit.meeting.modules.ai.service.RecordingSubtitleRetranslationService
      retranslation;

  @GetMapping("/review")
  public BaseResponse<RecordingSubtitleReviewResponse> get(
      @PathVariable String recordingId,
      @AuthenticationPrincipal(expression = "userId") Long userId) {
    return BaseResponse.success(service.get(recordingId, userId));
  }

  @PostMapping("/review/load-latest")
  public BaseResponse<RecordingSubtitleReviewResponse> load(
      @PathVariable String recordingId,
      @AuthenticationPrincipal(expression = "userId") Long userId,
      @Valid @RequestBody Revision request) {
    return BaseResponse.success(
        service.loadLatest(recordingId, userId, request.expectedRevision()));
  }

  @PutMapping("/review")
  public BaseResponse<RecordingSubtitleReviewResponse> save(
      @PathVariable String recordingId,
      @AuthenticationPrincipal(expression = "userId") Long userId,
      @Valid @RequestBody Save request) {
    return BaseResponse.success(service.save(recordingId, userId, request));
  }

  @PostMapping("/publish")
  public BaseResponse<RecordingSubtitleReviewResponse> publish(
      @PathVariable String recordingId,
      @AuthenticationPrincipal(expression = "userId") Long userId,
      @Valid @RequestBody Revision request) {
    return BaseResponse.success(service.publish(recordingId, userId, request.expectedRevision()));
  }

  @DeleteMapping
  public BaseResponse<Boolean> delete(
      @PathVariable String recordingId,
      @AuthenticationPrincipal(expression = "userId") Long userId,
      @Valid @RequestBody Revision request) {
    service.delete(recordingId, userId, request.expectedRevision());
    return BaseResponse.success(true);
  }

  @PostMapping("/review/retranslate")
  public BaseResponse<RecordingSubtitleReviewResponse> retranslate(
      @PathVariable String recordingId,
      @AuthenticationPrincipal(expression = "userId") Long userId,
      @Valid @RequestBody
          org.ptit.meeting.modules.ai.dto.request.RecordingSubtitleReviewRequest.Retranslate
              request) {
    return BaseResponse.success(
        retranslation.translate(
            recordingId, userId, request.expectedRevision(), request.segmentId()));
  }

  @GetMapping
  public BaseResponse<JsonNode> published(
      @PathVariable String recordingId,
      @AuthenticationPrincipal(expression = "userId") Long userId) {
    return BaseResponse.success(service.published(recordingId, userId));
  }
}
