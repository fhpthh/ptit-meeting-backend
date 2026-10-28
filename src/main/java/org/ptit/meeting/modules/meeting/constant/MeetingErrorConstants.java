package org.ptit.meeting.modules.meeting.constant;

/**
 * Message keys for business errors owned by the meeting module.
 */
public final class MeetingErrorConstants {

  public static final String ROOM_NOT_FOUND = "error.meeting.room.not-found";
  public static final String ROOM_ARCHIVED = "error.meeting.room.archived";
  public static final String ROOM_NAME_INVALID = "error.meeting.room.name-invalid";
  public static final String ROOM_DESCRIPTION_INVALID = "error.meeting.room.description-invalid";
  public static final String ROOM_TYPE_INVALID = "error.meeting.room.type-invalid";
  public static final String SETTING_NOT_FOUND = "error.meeting.setting.not-found";
  public static final String SETTING_INVALID = "error.meeting.setting.invalid";
  public static final String MEMBER_NOT_FOUND = "error.meeting.member.not-found";
  public static final String MEMBER_ALREADY_EXISTS = "error.meeting.member.already-exists";
  public static final String MEMBER_ROLE_INVALID = "error.meeting.member.role-invalid";
  public static final String OWNER_IMMUTABLE = "error.meeting.member.owner-immutable";
  public static final String VIEWER_EVENT_ONLY = "error.meeting.member.viewer-event-only";
  public static final String RUNTIME_SESSION_NOT_FOUND =
      "error.meeting.runtime-session.not-found";
  public static final String RUNTIME_SESSION_TITLE_INVALID =
      "error.meeting.runtime-session.title-invalid";
  public static final String RUNTIME_SESSION_TIME_INVALID =
      "error.meeting.runtime-session.time-invalid";
  public static final String RUNTIME_SESSION_STATE_INVALID =
      "error.meeting.runtime-session.state-invalid";
  public static final String RUNTIME_SESSION_SCHEDULE_INVALID =
      "error.meeting.runtime-session.schedule-invalid";
  public static final String RUNTIME_SESSION_SCHEDULE_CONFLICT =
      "error.meeting.runtime-session.schedule-conflict";
  public static final String RUNTIME_SESSION_CONCURRENT_UPDATE =
      "error.meeting.runtime-session.concurrent-update";
  public static final String RUNTIME_SESSION_NOT_ACTIVE =
      "error.meeting.runtime-session.not-active";
  public static final String JOIN_TOO_EARLY = "error.meeting.join.too-early";
  public static final String JOIN_FAILED = "error.meeting.join.failed";
  public static final String PROVIDER_UNAVAILABLE = "error.meeting.provider-unavailable";
  public static final String WEBHOOK_SIGNATURE_INVALID =
      "error.meeting.webhook.signature-invalid";
  public static final String WEBHOOK_PAYLOAD_INVALID =
      "error.meeting.webhook.payload-invalid";
  public static final String ACCESS_DENIED = "error.meeting.access-denied";
  public static final String MODERATION_SELF_ACTION =
      "error.meeting.moderation.self-action";
  public static final String MODERATION_TARGET_PROTECTED =
      "error.meeting.moderation.target-protected";
  public static final String MODERATION_PARTICIPANT_NOT_CONNECTED =
      "error.meeting.moderation.participant-not-connected";
  public static final String SESSION_PARTICIPANT_BANNED =
      "error.meeting.moderation.participant-banned";
  public static final String SESSION_BAN_NOT_FOUND =
      "error.meeting.moderation.ban-not-found";
  public static final String CHAT_MESSAGE_INVALID =
      "error.meeting.chat.message-invalid";
  public static final String CHAT_CURSOR_INVALID =
      "error.meeting.chat.cursor-invalid";
  public static final String CHAT_HISTORY_LIMIT_INVALID =
      "error.meeting.chat.history-limit-invalid";
  public static final String CHAT_RATE_LIMIT_EXCEEDED =
      "error.meeting.chat.rate-limit-exceeded";
  public static final String CHAT_UNAVAILABLE =
      "error.meeting.chat.unavailable";

  private MeetingErrorConstants() {
  }
}
