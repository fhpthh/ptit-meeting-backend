package org.ptit.meeting.exception;

import java.util.Map;

public class BusinessException extends BaseException {

  public BusinessException(ErrorCode errorCode) {
    super(errorCode);
  }

  public BusinessException(ErrorCode errorCode, Map<String, Object> params) {
    super(errorCode, params);
  }

  public BusinessException(ErrorCode errorCode, String customMessage) {
    super(errorCode, customMessage);
  }

  public BusinessException(ErrorCode errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}
