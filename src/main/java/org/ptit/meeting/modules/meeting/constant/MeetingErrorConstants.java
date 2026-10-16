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
  public static final String SESSION_NOT_FOUND = "error.meeting.session.not-found";
  public static final String SESSION_TITLE_INVALID = "error.meeting.session.title-invalid";
  public static final String SESSION_TIME_INVALID = "error.meeting.session.time-invalid";
  public static final String SESSION_STATE_INVALID = "error.meeting.session.state-invalid";
  public static final String SESSION_SCHEDULE_INVALID = "error.meeting.session.schedule-invalid";
  public static final String SESSION_SCHEDULE_CONFLICT = "error.meeting.session.schedule-conflict";
  public static final String SESSION_CONCURRENT_UPDATE = "error.meeting.session.concurrent-update";
  public static final String ACCESS_DENIED = "error.meeting.access-denied";

  private MeetingErrorConstants() {
  }
}
