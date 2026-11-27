package org.ptit.meeting.modules.ai.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.ai.dto.request.RecordingTranslationCreateRequest;
import org.ptit.meeting.modules.ai.dto.response.RecordingTranslationJobResponse;
import org.ptit.meeting.modules.ai.service.RecordingTranslationJobService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meeting-recordings/{recordingId}/translations")
@RequiredArgsConstructor
public class RecordingTranslationJobController {
  private final RecordingTranslationJobService service;

  @PostMapping
  public BaseResponse<RecordingTranslationJobResponse> create(
      @PathVariable String recordingId,
      @RequestHeader("Idempotency-Key") String key,
      @Valid @RequestBody RecordingTranslationCreateRequest request,
      @AuthenticationPrincipal(expression = "userId") Long userId) {
    return BaseResponse.created(service.create(recordingId, userId, key, request));
  }

  @GetMapping
  public BaseResponse<List<RecordingTranslationJobResponse>> list(
      @PathVariable String recordingId,
      @AuthenticationPrincipal(expression = "userId") Long userId) {
    return BaseResponse.success(service.list(recordingId, userId));
  }

  @GetMapping("/{jobId}")
  public BaseResponse<RecordingTranslationJobResponse> get(
      @PathVariable String recordingId,
      @PathVariable String jobId,
      @AuthenticationPrincipal(expression = "userId") Long userId) {
    return BaseResponse.success(service.get(recordingId, jobId, userId));
  }

  @PostMapping("/{jobId}/cancel")
  public BaseResponse<RecordingTranslationJobResponse> cancel(
      @PathVariable String recordingId,
      @PathVariable String jobId,
      @AuthenticationPrincipal(expression = "userId") Long userId) {
    return BaseResponse.success(service.cancel(recordingId, jobId, userId));
  }
}
