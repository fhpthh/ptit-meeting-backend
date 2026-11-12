package org.ptit.meeting.modules.meeting.event;

/** Reconcile current membership and media grants after the transaction commits. */
public record MeetingRoomAccessChangedEvent(String roomId) {
}
