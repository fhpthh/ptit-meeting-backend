package org.ptit.meeting.util;

public class MessageKeyConstant {

  public static final String COMMON_SUCCESS = "common.success";
  public static final String COMMON_CREATED = "common.created";
  public static final String VALIDATION_INVALID_INPUT = "validation.invalid_input";
  public static final String SYSTEM_INTERNAL_ERROR = "system.internal_error";
  public static final String RESOURCE_NOT_FOUND = "resource.not_found";
  public static final String ATTACHMENT_FILE_TOO_LARGE = "attachment.file_too_large";

  // Auth messages & errors
  public static final String AUTH_FORBIDDEN = "auth.forbidden";
  public static final String AUTH_UNAUTHORIZED = "auth.unauthorized";
  public static final String AUTH_INVALID_CREDENTIALS = "auth.invalid_credentials";
  public static final String AUTH_ACCOUNT_LOCKED = "auth.account_locked";
  public static final String AUTH_INVALID_DOMAIN = "auth.invalid_domain";
  public static final String AUTH_RATE_LIMIT_EXCEEDED = "auth.rate_limit_exceeded";
  public static final String AUTH_TOKEN_EXPIRED = "auth.token_expired";
  public static final String AUTH_TOKEN_COMPROMISED = "auth.token_compromised";
  public static final String AUTH_LOGIN_SUCCESS = "auth.login_success";
  public static final String AUTH_MUST_CHANGE_PASSWORD = "auth.must_change_password";
  public static final String AUTH_CHANGE_PASSWORD_SUCCESS = "auth.change_password_success";
  public static final String AUTH_GET_PROFILE_SUCCESS = "auth.get_profile_success";
  public static final String AUTH_OUTLOOK_LOGIN_SUCCESS = "auth.outlook_login_success";
  public static final String AUTH_OUTLOOK_URL_SUCCESS = "auth.outlook_url_success";
  public static final String AUTH_REFRESH_TOKEN_SUCCESS = "auth.refresh_token_success";
  public static final String AUTH_LOGOUT_SUCCESS = "auth.logout_success";

  private MessageKeyConstant() {
  }
}
