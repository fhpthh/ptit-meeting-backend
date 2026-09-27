package org.ptit.meeting.modules.auth.constant;

public final class AuthErrorConstants {

  public static final String UNAUTHORIZED = "auth.unauthorized";
  public static final String FORBIDDEN = "auth.forbidden";
  public static final String INVALID_CREDENTIALS = "auth.invalid_credentials";
  public static final String ACCOUNT_LOCKED = "auth.account_locked";
  public static final String INVALID_DOMAIN = "auth.invalid_domain";
  public static final String RATE_LIMIT_EXCEEDED = "auth.rate_limit_exceeded";
  public static final String TOKEN_EXPIRED = "auth.token_expired";
  public static final String TOKEN_COMPROMISED = "auth.token_compromised";
  public static final String MUST_CHANGE_PASSWORD = "auth.must_change_password";

  private AuthErrorConstants() {
  }
}
