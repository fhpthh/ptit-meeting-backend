package org.ptit.meeting.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.util.MessageKeyConstant;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  SYSTEM_INTERNAL_ERROR("SYS_001", MessageKeyConstant.SYSTEM_INTERNAL_ERROR,
      HttpStatus.INTERNAL_SERVER_ERROR),
  VALIDATION_INVALID_INPUT("SYS_002", MessageKeyConstant.VALIDATION_INVALID_INPUT,
      HttpStatus.BAD_REQUEST),
  RESOURCE_NOT_FOUND("SYS_003", MessageKeyConstant.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND),
  ATTACHMENT_FILE_TOO_LARGE("SYS_004", MessageKeyConstant.ATTACHMENT_FILE_TOO_LARGE,
      HttpStatus.PAYLOAD_TOO_LARGE),

  // Xác thực & Phân quyền (Auth & RBAC)
  AUTH_UNAUTHORIZED("AUTH_001", MessageKeyConstant.AUTH_UNAUTHORIZED, HttpStatus.UNAUTHORIZED),
  AUTH_FORBIDDEN("AUTH_002", MessageKeyConstant.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN),
  AUTH_INVALID_CREDENTIALS("AUTH_003", MessageKeyConstant.AUTH_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED),
  AUTH_ACCOUNT_LOCKED("AUTH_004", MessageKeyConstant.AUTH_ACCOUNT_LOCKED, HttpStatus.LOCKED),
  AUTH_INVALID_DOMAIN("AUTH_005", MessageKeyConstant.AUTH_INVALID_DOMAIN, HttpStatus.FORBIDDEN),
  AUTH_RATE_LIMIT_EXCEEDED("AUTH_006", MessageKeyConstant.AUTH_RATE_LIMIT_EXCEEDED, HttpStatus.TOO_MANY_REQUESTS),
  AUTH_TOKEN_EXPIRED("AUTH_007", MessageKeyConstant.AUTH_TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED),
  AUTH_TOKEN_COMPROMISED("AUTH_008", MessageKeyConstant.AUTH_TOKEN_COMPROMISED, HttpStatus.UNAUTHORIZED),
  AUTH_MUST_CHANGE_PASSWORD("AUTH_009", MessageKeyConstant.AUTH_MUST_CHANGE_PASSWORD, HttpStatus.OK);

  private final String code;
  private final String message;
  private final HttpStatus status;
}
