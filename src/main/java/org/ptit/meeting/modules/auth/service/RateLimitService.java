
package org.ptit.meeting.modules.auth.service;

public interface RateLimitService {

  void checkRateLimit(String identifier);

  void recordLoginFailure(String identifier);

  void resetLoginFailures(String identifier);
}
