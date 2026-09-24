package org.ptit.meeting.layer.domain.exception.business;

import org.ptit.meeting.layer.domain.exception.BusinessException;
import org.ptit.meeting.layer.domain.exception.enums.ErrorCode;

public class InvalidDomainException extends BusinessException {

  public InvalidDomainException() {
    super(ErrorCode.AUTH_INVALID_DOMAIN);
  }

}
