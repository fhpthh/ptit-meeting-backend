package org.ptit.meeting.modules.ai.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.config.AiRecordingProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.ai.constant.AiErrorConstants;
import org.ptit.meeting.modules.ai.dto.request.RecordingSubtitleReviewRequest.Save;
import org.ptit.meeting.modules.ai.dto.response.RecordingSubtitleReviewResponse;
import org.ptit.meeting.modules.ai.entity.RecordingSubtitleReview;
import org.ptit.meeting.modules.ai.repository.RecordingSubtitleReviewRepository;
import org.ptit.meeting.modules.ai.repository.RecordingTranslationDraftRepository;
import org.ptit.meeting.modules.ai.service.RecordingSubtitleReviewService;
import org.ptit.meeting.modules.meeting.service.MeetingRecordingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
@RequiredArgsConstructor
public class RecordingSubtitleReviewServiceImpl implements RecordingSubtitleReviewService {
  private final RecordingSubtitleReviewRepository reviews;
  private final RecordingTranslationDraftRepository drafts;
  private final MeetingRecordingService recordings;
  private final AiRecordingProperties properties;
  private final ObjectMapper json;
  private final org.ptit.meeting.modules.auth.service.UserService users;

  private void manager(String id, Long userId) {
    var user = users.getUserById(userId);
    if (user == null
        || user.getAccountType()
            != org.ptit.meeting.modules.auth.constant.enums.AccountType.LECTURER) {
      throw BusinessException.unauthorized(AiErrorConstants.RECORDING_ACCESS);
    }
    users.validateUserStatus(user);
    recordings.requireTranslationSource(id, userId);
  }

  private void enabled() {
    if (!properties.isEnabled())
      throw BusinessException.invalidInput(AiErrorConstants.RECORDING_DISABLED);
  }

  private BusinessException conflict() {
    return new BusinessException(StatusCode.CONFLICT, AiErrorConstants.RECORDING_CONFLICT);
  }

  private JsonNode parse(String value) {
    try {
      return json.readTree(value);
    } catch (Exception e) {
      throw new IllegalStateException("Invalid persisted subtitles");
    }
  }

  private String write(JsonNode value) {
    try {
      return json.writeValueAsString(value);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot serialize subtitles");
    }
  }

  private RecordingSubtitleReview require(String id, long revision) {
    var r = reviews.findByRecordingId(id).orElseThrow(this::conflict);
    if (r.getRevision() != revision || r.isDeleted()) throw conflict();
    return r;
  }

  private RecordingSubtitleReviewResponse response(RecordingSubtitleReview r) {
    return new RecordingSubtitleReviewResponse(
        r.getDraftId(),
        r.getRevision(),
        r.getPublishedRevision(),
        r.isDeleted(),
        r.isDeleted() ? null : parse(r.getDocumentJson()));
  }

  @Override
  @Transactional
  public RecordingSubtitleReviewResponse get(String id, Long user) {
    enabled();
    manager(id, user);
    return reviews.findByRecordingId(id).map(this::response).orElse(null);
  }

  @Override
  @Transactional
  public RecordingSubtitleReviewResponse loadLatest(String id, Long user, long revision) {
    enabled();
    manager(id, user);
    var existing = reviews.findByRecordingId(id);
    if (existing.map(RecordingSubtitleReview::getRevision).orElse(0L) != revision) throw conflict();
    var draft =
        drafts
            .findFirstByRecordingIdOrderByCreatedAtDescIdDesc(id)
            .orElseThrow(
                () -> BusinessException.notExisted(AiErrorConstants.RECORDING_JOB_NOT_FOUND));
    var r = existing.orElseGet(RecordingSubtitleReview::new);
    r.setRecordingId(id);
    r.setDraftId(draft.getId());
    r.setDocumentJson(draft.getOriginalDocumentJson());
    r.setDeleted(false);
    r.setRevision(revision + 1);
    reviews.saveAndFlush(r);
    return response(r);
  }

  @Override
  @Transactional
  public RecordingSubtitleReviewResponse save(String id, Long user, Save request) {
    enabled();
    manager(id, user);
    var r = require(id, request.expectedRevision());
    JsonNode doc = parse(r.getDocumentJson());
    Map<String, ObjectNode> segments = new HashMap<>();
    for (JsonNode row : doc.path("segments")) {
      segments.put(
          row.path("segmentId").asText() + ":" + row.path("language").asText(), (ObjectNode) row);
    }
    Set<String> changed = new HashSet<>();
    for (var edit : request.edits()) {
      String key = edit.segmentId() + ":" + edit.language();
      var row = segments.get(key);
      if (row == null || !changed.add(key))
        throw BusinessException.invalidInput(AiErrorConstants.RECORDING_INVALID);
      row.put("text", edit.text());
      row.put("revision", row.path("revision").asLong() + 1);
    }
    r.setRevision(r.getRevision() + 1);
    ((ObjectNode) doc).put("documentRevision", r.getRevision());
    r.setDocumentJson(write(doc));
    reviews.saveAndFlush(r);
    return response(r);
  }

  @Override
  @Transactional
  public RecordingSubtitleReviewResponse publish(String id, Long user, long revision) {
    enabled();
    manager(id, user);
    var r = require(id, revision);
    r.setPublishedJson(r.getDocumentJson());
    r.setPublishedRevision(revision);
    reviews.saveAndFlush(r);
    return response(r);
  }

  @Override
  @Transactional
  public void delete(String id, Long user, long revision) {
    enabled();
    manager(id, user);
    var r = require(id, revision);
    r.setPublishedJson(null);
    r.setPublishedRevision(null);
    r.setDeleted(true);
    r.setRevision(revision + 1);
    reviews.saveAndFlush(r);
  }

  @Override
  @Transactional(readOnly = true)
  public JsonNode published(String id, Long user) {
    enabled();
    recordings.requireSubtitleViewAccess(id, user);
    return reviews
        .findByRecordingId(id)
        .filter(r -> !r.isDeleted() && r.getPublishedJson() != null)
        .map(r -> parse(r.getPublishedJson()))
        .orElse(null);
  }
}
