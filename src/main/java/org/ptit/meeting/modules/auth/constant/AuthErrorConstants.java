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
  public static final String SHORT_CODE_NOT_ALLOWED = "auth.short_code_not_allowed";
  public static final String STUDENT_NOT_FOUND = "error.student-admin.not-found";
  public static final String STUDENT_EMAIL_EXISTS = "error.student-admin.email-exists";
  public static final String STUDENT_CODE_EXISTS = "error.student-admin.code-exists";
  public static final String STUDENT_CODE_INVALID = "validation.student-admin.code.invalid";
  public static final String STUDENT_ROLE_MISSING = "error.student-admin.role-missing";
  public static final String STUDENT_SORT_INVALID = "error.student-admin.sort-invalid";
  public static final String STUDENT_EXPORT_LIMIT = "error.student-admin.export-limit";
  public static final String STUDENT_EXPORT_FAILED = "error.student-admin.export-failed";
  public static final String STUDENT_PASSWORD_SIZE = "validation.student-admin.password.size";
  public static final String LECTURER_IMPORT_EMPTY_FILE = "error.lecturer.import.empty-file";
  public static final String LECTURER_IMPORT_INVALID_FORMAT = "error.lecturer.import.invalid-format";
  public static final String LECTURER_ROLE_MISSING = "error.lecturer.role-missing";

  private AuthErrorConstants() {
  }
}
