package org.ptit.meeting.layer.domain.exception.business;

import org.ptit.meeting.layer.domain.exception.BusinessException;
import org.ptit.meeting.layer.domain.exception.enums.ErrorCode;

public class AccountLockedException extends BusinessException {

  public AccountLockedException() {
    super(ErrorCode.AUTH_ACCOUNT_LOCKED);
  }
}
