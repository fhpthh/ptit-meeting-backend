package org.ptit.meeting.modules.meeting.service;

import java.time.Instant;

public interface MeetingHostPresenceService {

  /** Capture before listing LiveKit participants; an empty string means no state yet. */
  String getState(String runtimeSessionId);

  /** Ignore the observation if a host return or another observation changed its state. */
  boolean hasReconnectGracePassed(String runtimeSessionId, String observedState, Instant now);

  void markPresent(String runtimeSessionId);

  void clear(String runtimeSessionId);
}
