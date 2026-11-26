package org.ptit.meeting.modules.ai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.ai.dto.internal.RecordingTrustedWorker;
import org.ptit.meeting.modules.ai.dto.request.RecordingProvisionRequest;
import org.ptit.meeting.modules.ai.dto.response.RecordingWorkerGrantResponse;
import org.ptit.meeting.modules.ai.service.RecordingWorkerProvisioningService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/recording-credentials")
@ConditionalOnProperty(
    prefix = "application.ai-recording.provisioning",
    name = "enabled",
    havingValue = "true")
public class RecordingWorkerProvisioningController {
  private final RecordingWorkerProvisioningService service;

  @PostMapping("/claim-grant")
  public BaseResponse<RecordingWorkerGrantResponse> claim(
      @AuthenticationPrincipal RecordingTrustedWorker worker,
      @Valid @RequestBody RecordingProvisionRequest request) {
    return BaseResponse.success(service.claim(worker, request));
  }
}
