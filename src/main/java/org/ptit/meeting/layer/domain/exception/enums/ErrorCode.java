package org.ptit.meeting.layer.domain.exception.enums;

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
  RESOURCE_NOT_FOUND("SYS_003", "resource.not_found", HttpStatus.NOT_FOUND),
  ATTACHMENT_FILE_TOO_LARGE("SYS_004", MessageKeyConstant.ATTACHMENT_FILE_TOO_LARGE,
      HttpStatus.PAYLOAD_TOO_LARGE),

  // Xác thực & Phân quyền (Auth & RBAC)
  AUTH_UNAUTHORIZED("AUTH_001", MessageKeyConstant.AUTH_UNAUTHORIZED, HttpStatus.UNAUTHORIZED),
  AUTH_FORBIDDEN("AUTH_002", MessageKeyConstant.AUTH_FORBIDDEN, HttpStatus.FORBIDDEN),
  AUTH_INVALID_CREDENTIALS("AUTH_003", "auth.invalid_credentials", HttpStatus.UNAUTHORIZED),
  AUTH_ACCOUNT_LOCKED("AUTH_004", "auth.account_locked", HttpStatus.LOCKED),
  AUTH_INVALID_DOMAIN("AUTH_005", "auth.invalid_domain", HttpStatus.FORBIDDEN),
  AUTH_RATE_LIMIT_EXCEEDED("AUTH_006", "auth.rate_limit_exceeded", HttpStatus.TOO_MANY_REQUESTS),
  AUTH_TOKEN_EXPIRED("AUTH_007", "auth.token_expired", HttpStatus.UNAUTHORIZED),
  AUTH_TOKEN_COMPROMISED("AUTH_008", "auth.token_compromised", HttpStatus.UNAUTHORIZED),
  AUTH_MUST_CHANGE_PASSWORD("AUTH_009", "auth.must_change_password", HttpStatus.OK);
  private final String code;
  private final String message;
  private final HttpStatus status;
}
