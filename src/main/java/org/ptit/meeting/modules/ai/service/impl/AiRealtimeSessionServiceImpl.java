package org.ptit.meeting.modules.ai.service.impl;

import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.config.AiRealtimeProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.integration.ai.AiRuntimeGateway;
import org.ptit.meeting.integration.ai.AiRuntimeIntegrationException;
import org.ptit.meeting.integration.ai.dto.AiRuntimeStartCommand;
import org.ptit.meeting.integration.ai.dto.AiRuntimeCourseContext;
import org.ptit.meeting.integration.ai.dto.AiRuntimeView;
import org.ptit.meeting.integration.livekit.LiveKitGateway;
import org.ptit.meeting.integration.livekit.dto.LiveKitJoinGrant;
import org.ptit.meeting.integration.livekit.dto.LiveKitTokenResult;
import org.ptit.meeting.modules.ai.constant.AiErrorConstants;
import org.ptit.meeting.modules.ai.dto.response.AiRealtimeSessionResponse;
import org.ptit.meeting.modules.ai.entity.AiRealtimeSession;
import org.ptit.meeting.modules.ai.repository.AiRealtimeSessionRepository;
import org.ptit.meeting.modules.ai.service.AiRealtimeSessionService;
import org.ptit.meeting.modules.meeting.constant.MeetingErrorConstants;
import org.ptit.meeting.modules.meeting.constant.enums.SessionStatus;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.ptit.meeting.modules.meeting.repository.MeetingRuntimeSessionRepository;
import org.ptit.meeting.modules.meeting.service.MeetingAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ptit.meeting.modules.timetable.service.ClassSectionSubjectService;

@Service
@RequiredArgsConstructor
public class AiRealtimeSessionServiceImpl implements AiRealtimeSessionService {

  private final AiRealtimeSessionRepository repository;
  private final MeetingRuntimeSessionRepository runtimeSessionRepository;
  private final MeetingAuthorizationService authorizationService;
  private final LiveKitGateway liveKitGateway;
  private final AiRealtimeProperties properties;
  private final AiRuntimeGateway gateway;
  private final ClassSectionSubjectService classSectionSubjectService;

  @Override
  @Transactional(noRollbackFor = BusinessException.class)
  public AiRealtimeSessionResponse start(String runtimeSessionId, Long currentUserId) {
    requireEnabled();
    MeetingSession runtime = findRuntime(runtimeSessionId);
    authorizationService.requireRuntimeSessionHost(runtime.getMeetingRoomId(), currentUserId);
    if (runtime.getStatus() != SessionStatus.LIVE) {
      throw BusinessException.invalidInput(MeetingErrorConstants.RUNTIME_SESSION_NOT_ACTIVE);
    }

    AiRealtimeSession session = repository.findByRuntimeSessionId(runtimeSessionId)
        .orElseGet(() -> new AiRealtimeSession(runtimeSessionId, 1, currentUserId));
    if (session.getStatus() == org.ptit.meeting.modules.ai.constant.enums.AiRealtimeStatus.ACTIVE) {
      try {
        AiRuntimeView runtimeView = gateway.status(runtimeSessionId);
        if (runtimeView.ready() && "ACTIVE".equals(runtimeView.state())) {
          return response(session);
        }
      } catch (AiRuntimeIntegrationException exception) {
        if (!exception.isNotFound()) {
          throw unavailable();
        }
      }
    }
    long epoch = session.getId() == null ? 1 : session.getEnableEpoch() + 1;
    if (session.getId() != null) session.restart(epoch, currentUserId);
    repository.saveAndFlush(session);

    LiveKitTokenResult workerToken = liveKitGateway.createJoinToken(new LiveKitJoinGrant(
        runtime.getProviderRoomName(),
        "ai-worker-" + runtimeSessionId,
        "PTIT AI Caption Worker",
        workerMetadata(runtimeSessionId, epoch),
        true, false, false, false, true));
    try {
      gateway.start(new AiRuntimeStartCommand(
          1,
          runtimeSessionId,
          epoch,
          runtime.getProviderRoomName(),
          runtime.getMeetingRoomId(),
          "user-" + currentUserId,
          currentUserId.toString(),
          workerToken.websocketUrl(),
          workerToken.participantToken(),
          properties.getCallbackBaseUrl() + "/internal/v1/ai-realtime/final-captions",
          properties.getServiceSecret(),
          courseContext(runtime)));
      session.activate();
      return response(repository.saveAndFlush(session));
    } catch (AiRuntimeIntegrationException exception) {
      session.fail(exception.getMessage());
      repository.saveAndFlush(session);
      throw unavailable();
    }
  }

  @Override
  @Transactional(noRollbackFor = BusinessException.class)
  public AiRealtimeSessionResponse stop(String runtimeSessionId, Long currentUserId) {
    requireEnabled();
    MeetingSession runtime = findRuntime(runtimeSessionId);
    authorizationService.requireRuntimeSessionHost(runtime.getMeetingRoomId(), currentUserId);
    AiRealtimeSession session = findAiSession(runtimeSessionId);
    if (session.getStatus() == org.ptit.meeting.modules.ai.constant.enums.AiRealtimeStatus.STOPPED) {
      return response(session);
    }
    try {
      gateway.stop(runtimeSessionId);
      session.stop();
      return response(repository.saveAndFlush(session));
    } catch (AiRuntimeIntegrationException exception) {
      if (exception.isNotFound()) {
        session.stop();
        return response(repository.saveAndFlush(session));
      }
      session.fail(exception.getMessage());
      repository.saveAndFlush(session);
      throw unavailable();
    }
  }

  @Override
  @Transactional(noRollbackFor = BusinessException.class)
  public AiRealtimeSessionResponse status(String runtimeSessionId, Long currentUserId) {
    MeetingSession runtime = findRuntime(runtimeSessionId);
    authorizationService.requireRoomAccess(runtime.getMeetingRoomId(), currentUserId);
    AiRealtimeSession session = findAiSession(runtimeSessionId);
    if (session.getStatus()
        == org.ptit.meeting.modules.ai.constant.enums.AiRealtimeStatus.ACTIVE) {
      try {
        AiRuntimeView view = gateway.status(runtimeSessionId);
        if (!view.ready() || !"ACTIVE".equals(view.state())) {
          session.fail("AI runtime is not ready");
          repository.saveAndFlush(session);
        }
      } catch (AiRuntimeIntegrationException exception) {
        if (exception.isNotFound()) {
          session.stop();
          repository.saveAndFlush(session);
        } else {
          throw unavailable();
        }
      }
    }
    return response(session);
  }

  @Override
  @Transactional
  public void stopAfterMeetingEnded(String runtimeSessionId) {
    repository.findByRuntimeSessionId(runtimeSessionId).ifPresent(session -> {
      if (session.getStatus()
          == org.ptit.meeting.modules.ai.constant.enums.AiRealtimeStatus.ACTIVE) {
        try {
          gateway.stop(runtimeSessionId);
          session.stop();
        } catch (AiRuntimeIntegrationException exception) {
          if (exception.isNotFound()) {
            session.stop();
          } else {
            session.fail(exception.getMessage());
          }
        }
        repository.saveAndFlush(session);
      }
    });
  }

  private void requireEnabled() {
    if (!properties.isEnabled()) throw BusinessException.invalidInput(AiErrorConstants.DISABLED);
  }

  private AiRuntimeCourseContext courseContext(MeetingSession runtime) {
    Long classSectionId = runtime.getRoom() == null ? null : runtime.getRoom().getClassSectionId();
    if (classSectionId == null) {
      return new AiRuntimeCourseContext("", "", properties.getTerminologyProfile(), 1);
    }
    return classSectionSubjectService.findSubject(classSectionId)
        .map(subject -> new AiRuntimeCourseContext(
            subject.code(), subject.name(),
            properties.getTerminologyProfiles().getOrDefault(
                subject.code(), properties.getTerminologyProfile()), 1))
        .orElseGet(() -> new AiRuntimeCourseContext("", "", properties.getTerminologyProfile(), 1));
  }

  private MeetingSession findRuntime(String id) {
    return runtimeSessionRepository.findByPublicId(id)
        .orElseThrow(() -> BusinessException.notExisted(MeetingErrorConstants.RUNTIME_SESSION_NOT_FOUND));
  }

  private AiRealtimeSession findAiSession(String id) {
    return repository.findByRuntimeSessionId(id)
        .orElseThrow(() -> BusinessException.notExisted(AiErrorConstants.NOT_FOUND));
  }

  private BusinessException unavailable() {
    return new BusinessException(StatusCode.ERROR_IN_BACKEND, AiErrorConstants.UNAVAILABLE);
  }

  private String workerMetadata(String runtimeSessionId, long epoch) {
    return "{\"kind\":\"ai-caption-worker\",\"schemaVersion\":1,"
        + "\"runtimeSessionId\":\"" + runtimeSessionId + "\","
        + "\"enableEpoch\":" + epoch + "}";
  }

  private AiRealtimeSessionResponse response(AiRealtimeSession session) {
    return new AiRealtimeSessionResponse(
        session.getRuntimeSessionId(), session.getEnableEpoch(), session.getStatus(),
        session.getStartedAt(), session.getStoppedAt());
  }
}
