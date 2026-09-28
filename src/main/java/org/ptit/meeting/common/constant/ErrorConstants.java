package org.ptit.meeting.common.constant;

public final class ErrorConstants {

  public static final String AUTHENTICATION_FAILED = "error.auth.token-invalid";
  public static final String ACCESS_DENIED = "error.auth.access-denied";
  public static final String BAD_CREDENTIALS = "error.auth.bad-credentials";
  public static final String DATA_EXISTED = "error.common.data-existed";
  public static final String DATA_NOT_FOUND = "error.common.data-not-found";
  public static final String INVALID_INPUT = "error.common.argument-not-valid";
  public static final String INVALID_INPUT_FILE = "error.common.invalid-input-file";
  public static final String ARGUMENT_TYPE_MISMATCH = "error.common.argument-type-mismatch";
  public static final String SERVER_ERROR = "error.common.internal-error";
  public static final String VALIDATION_FAILED = "error.common.validation-failed";

  private ErrorConstants() {
  }
}
