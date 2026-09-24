package org.ptit.meeting.exception;

import org.ptit.meeting.common.constant.StatusCode;

public class BusinessException extends RuntimeException {

  private final StatusCode statusCode;
  private final String strCode;
  private final Object[] args;

  public BusinessException(StatusCode statusCode, String strCode, Object... args) {
    super(strCode);
    this.statusCode = statusCode;
    this.strCode = strCode;
    this.args = args == null ? new Object[0] : args.clone();
  }

  public static BusinessException invalidInput(String strCode, Object... args) {
    return new BusinessException(StatusCode.INVALID_INPUT_DATA, strCode, args);
  }

  public static BusinessException invalidInputFile(String strCode, Object... args) {
    return new BusinessException(StatusCode.INVALID_INPUT_FILE, strCode, args);
  }

  public static BusinessException existed(String strCode, Object... args) {
    return new BusinessException(StatusCode.EXISTED, strCode, args);
  }

  public static BusinessException notExisted(String strCode, Object... args) {
    return new BusinessException(StatusCode.NOT_EXISTED, strCode, args);
  }

  public static BusinessException unauthorized(String strCode, Object... args) {
    return new BusinessException(StatusCode.AUTHORIZATION_FAILED, strCode, args);
  }

  public StatusCode getStatusCode() {
    return statusCode;
  }

  public String getStrCode() {
    return strCode;
  }

  public Object[] getArgs() {
    return args.clone();
  }
}
