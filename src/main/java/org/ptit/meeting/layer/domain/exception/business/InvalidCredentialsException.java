package org.ptit.meeting.layer.domain.exception.business;

import org.ptit.meeting.layer.domain.exception.BusinessException;
import org.ptit.meeting.layer.domain.exception.enums.ErrorCode;

public class InvalidCredentialsException extends BusinessException {

  public InvalidCredentialsException() {
    super(ErrorCode.AUTH_INVALID_CREDENTIALS);
  }
}
