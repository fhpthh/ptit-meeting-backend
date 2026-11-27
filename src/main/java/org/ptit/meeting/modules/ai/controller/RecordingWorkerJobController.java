package org.ptit.meeting.modules.ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.ai.dto.internal.RecordingWorkerIdentity;
import org.ptit.meeting.modules.ai.dto.request.RecordingWorkerRequest.ClaimRequest;
import org.ptit.meeting.modules.ai.dto.request.RecordingWorkerRequest.FailureRequest;
import org.ptit.meeting.modules.ai.dto.request.RecordingWorkerRequest.ProgressRequest;
import org.ptit.meeting.modules.ai.dto.response.RecordingTranslationJobResponse;
import org.ptit.meeting.modules.ai.dto.response.RecordingWorkerGrantResponse;
import org.ptit.meeting.modules.ai.service.RecordingTranslationJobService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/recording-jobs/{jobId}")
@RequiredArgsConstructor
public class RecordingWorkerJobController {
  private final RecordingTranslationJobService service;

  @PostMapping("/claim")
  public BaseResponse<RecordingWorkerGrantResponse> claim(
      @PathVariable String jobId,
      @AuthenticationPrincipal RecordingWorkerIdentity worker,
      @Valid @RequestBody ClaimRequest request) {
    return BaseResponse.success(service.claim(jobId, worker, request));
  }

  @PostMapping("/heartbeat")
  public BaseResponse<RecordingWorkerGrantResponse> heartbeat(
      @PathVariable String jobId, @AuthenticationPrincipal RecordingWorkerIdentity worker) {
    return BaseResponse.success(service.heartbeat(jobId, worker));
  }

  @PostMapping("/progress")
  public BaseResponse<RecordingWorkerGrantResponse> progress(
      @PathVariable String jobId,
      @AuthenticationPrincipal RecordingWorkerIdentity worker,
      @Valid @RequestBody ProgressRequest request) {
    return BaseResponse.success(service.progress(jobId, worker, request));
  }

  @PostMapping("/failed")
  public BaseResponse<RecordingTranslationJobResponse> failed(
      @PathVariable String jobId,
      @AuthenticationPrincipal RecordingWorkerIdentity worker,
      @Valid @RequestBody FailureRequest request) {
    return BaseResponse.success(service.fail(jobId, worker, request));
  }
}
