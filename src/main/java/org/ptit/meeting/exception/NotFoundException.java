package org.ptit.meeting.exception;

import org.ptit.meeting.common.constant.StatusCode;

public class NotFoundException extends BusinessException {

  public NotFoundException(String strCode, Object... args) {
    super(StatusCode.NOT_EXISTED, strCode, args);
  }
}
