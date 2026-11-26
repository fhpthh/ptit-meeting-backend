package org.ptit.meeting.modules.ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.ai.dto.internal.RecordingWorkerIdentity;
import org.ptit.meeting.modules.ai.dto.request.RecordingResultCompleteRequest;
import org.ptit.meeting.modules.ai.dto.response.RecordingTranslationResultResponse;
import org.ptit.meeting.modules.ai.service.RecordingTranslationResultService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/recording-jobs/{jobId}")
@ConditionalOnProperty(
    prefix = "application.ai-recording",
    name = "completion-enabled",
    havingValue = "true")
public class RecordingWorkerResultController {
  private final RecordingTranslationResultService results;

  @PostMapping("/complete")
  public BaseResponse<RecordingTranslationResultResponse> complete(
      @PathVariable String jobId,
      @AuthenticationPrincipal RecordingWorkerIdentity worker,
      @Valid @RequestBody RecordingResultCompleteRequest request) {
    return BaseResponse.success(results.complete(jobId, worker, request));
  }
}
