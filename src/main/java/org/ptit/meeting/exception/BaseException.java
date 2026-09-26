package org.ptit.meeting.exception;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

  private final ErrorCode errorCode;
  private final Map<String, Object> params;

  public BaseException(ErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.params = new HashMap<>();
  }

  public BaseException(ErrorCode errorCode, Map<String, Object> params) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.params = params != null ? params : new HashMap<>();
  }

  public BaseException(ErrorCode errorCode, String customMessage) {
    super(customMessage);
    this.errorCode = errorCode;
    this.params = new HashMap<>();
  }

  public BaseException(ErrorCode errorCode, Throwable cause) {
    super(errorCode.getMessage(), cause);
    this.errorCode = errorCode;
    this.params = new HashMap<>();
  }
}
