package org.ptit.meeting.common.constant;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public enum StatusCode {
  SUCCESSFUL(0, "SUCCESSFUL", HttpStatus.OK),
  TRANSACTION_DONE(1, "TRANSACTION_DONE", HttpStatus.OK),
  INVALID_JSON_DATA(2, "INVALID_JSON_DATA", HttpStatus.BAD_REQUEST),
  INVALID_INPUT_DATA(3, "INVALID_INPUT_DATA", HttpStatus.BAD_REQUEST),
  LACK_OF_INPUT_DATA(4, "LACK_OF_INPUT_DATA", HttpStatus.BAD_REQUEST),
  INVALID_INPUT_FILE(5, "INVALID_INPUT_FILE", HttpStatus.BAD_REQUEST),
  INVALID_TOKEN(6, "INVALID_TOKEN", HttpStatus.UNAUTHORIZED),
  EXPIRED_TOKEN(7, "EXPIRED_TOKEN", HttpStatus.UNAUTHORIZED),
  AUTHENTICATION_FAILED(8, "AUTHENTICATION_FAILED", HttpStatus.UNAUTHORIZED),
  AUTHORIZATION_FAILED(9, "AUTHORIZATION_FAILED", HttpStatus.FORBIDDEN),
  ERROR_IN_BACKEND(10, "ERROR_IN_BACKEND", HttpStatus.INTERNAL_SERVER_ERROR),
  NOT_EXISTED(11, "NOT_EXISTED", HttpStatus.NOT_FOUND),
  INACTIVE(12, "INACTIVE", HttpStatus.BAD_REQUEST),
  EXISTED(13, "EXISTED", HttpStatus.BAD_REQUEST),
  UNABLE_TO_CHANGE_STATUS(14, "UNABLE_TO_CHANGE_STATUS", HttpStatus.BAD_REQUEST),
  UNABLE_TO_DELETE(15, "UNABLE_TO_DELETE", HttpStatus.BAD_REQUEST),
  EMPTY_DATA(16, "EMPTY_DATA", HttpStatus.BAD_REQUEST),
  VALIDATION_FAILED(17, "VALIDATION_FAILED", HttpStatus.BAD_REQUEST);

  private final int code;
  private final String message;
  private final HttpStatusCode httpStatusCode;

  StatusCode(int code, String message, HttpStatusCode httpStatusCode) {
    this.code = code;
    this.message = message;
    this.httpStatusCode = httpStatusCode;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public HttpStatusCode getHttpStatusCode() {
    return httpStatusCode;
  }
}
