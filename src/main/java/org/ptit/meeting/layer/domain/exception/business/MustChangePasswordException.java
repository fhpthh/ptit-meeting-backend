package org.ptit.meeting.layer.domain.exception.business;

import org.ptit.meeting.layer.domain.exception.BusinessException;
import org.ptit.meeting.layer.domain.exception.enums.ErrorCode;

public class MustChangePasswordException extends BusinessException {

  public MustChangePasswordException() {
    super(ErrorCode.AUTH_MUST_CHANGE_PASSWORD);
  }

}
