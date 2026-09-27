package org.ptit.meeting.service;

public interface RateLimitService {

  void checkRateLimit(String identifier);

  void recordLoginFailure(String identifier);

  void resetLoginFailures(String identifier);
}
