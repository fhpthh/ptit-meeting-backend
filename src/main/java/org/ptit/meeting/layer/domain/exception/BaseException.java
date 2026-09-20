package org.ptit.meeting.layer.domain.exception;

import lombok.Getter;
import org.ptit.meeting.layer.domain.exception.enums.ErrorCode;

@Getter
public class BaseException extends RuntimeException {

  private final ErrorCode errorCode;
  private final Object[] params;

  protected BaseException(ErrorCode errorCode, Object... params) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
    this.params = params;
  }

  protected BaseException(ErrorCode errorCode, String customMessage, Object... params) {
    super(customMessage);
    this.errorCode = errorCode;
    this.params = params;
  }
}
