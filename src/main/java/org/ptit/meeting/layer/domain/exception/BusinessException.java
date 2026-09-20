package org.ptit.meeting.layer.domain.exception;

import org.ptit.meeting.layer.domain.exception.enums.ErrorCode;

public class BusinessException extends BaseException{

  public BusinessException(ErrorCode errorCode, Object... params) {
    super(errorCode, params);
  }
  public BusinessException(ErrorCode errorCode, String customMessage, Object... params) {
    super(errorCode, customMessage, params);
  }
}
