package org.ptit.meeting.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.dto.response.ResponseGeneral;
import org.ptit.meeting.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final MessageService messageService;

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ResponseGeneral<Object>> handleSpringAccessDeniedException(
      final AccessDeniedException e, HttpServletRequest request) {
    log.error("(handleSpringAccessDeniedException) Access denied: {}", e.getMessage());

    ErrorCode errorCode = ErrorCode.AUTH_FORBIDDEN;
    String localizedMessage = messageService.getMessage(errorCode.getMessage());

    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        null
    );
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ResponseGeneral<Object>> handleAuthenticationException(
      final AuthenticationException e, HttpServletRequest request) {
    log.warn("(handleAuthenticationException) Authentication failed: {}", e.getMessage());

    ErrorCode errorCode = ErrorCode.AUTH_UNAUTHORIZED;
    String localizedMessage = messageService.getMessage(errorCode.getMessage());

    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        null
    );
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ResponseGeneral<Object>> handleBaseException(
      final BaseException e, HttpServletRequest request) {
    ErrorCode errorCode = e.getErrorCode();
    if (errorCode.getStatus().is5xxServerError()) {
      log.error("(handleBaseException) Internal application exception", e);
    } else {
      log.warn("(handleBaseException) Business exception: {}", e.getMessage());
    }

    String localizedMessage = messageService.getMessage(errorCode.getMessage(), e.getParams());
    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        null
    );
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ResponseGeneral<Object>> handleMethodArgumentNotValidException(
      final MethodArgumentNotValidException e, HttpServletRequest request) {
    log.error("(handleMethodArgumentNotValidException) Validation exception: {}", e.getMessage());
    Map<String, String> fieldErrors = new HashMap<>();
    List<FieldError> fieldErrorList = e.getBindingResult().getFieldErrors();
    for (FieldError fieldError : fieldErrorList) {
      String localizedFieldMsg = messageService.getMessage(fieldError.getDefaultMessage());
      fieldErrors.put(fieldError.getField(), localizedFieldMsg);
    }

    ErrorCode errorCode = ErrorCode.VALIDATION_INVALID_INPUT;
    String localizedMessage = messageService.getMessage(errorCode.getMessage());

    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        fieldErrors
    );
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ResponseGeneral<Object>> handleConstraintViolationException(
      final ConstraintViolationException e, HttpServletRequest request) {
    log.error("(handleConstraintViolationException) Constraint violation error occurred");

    Map<String, String> fieldErrors = new HashMap<>();
    for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
      String propertyPath = violation.getPropertyPath().toString();
      String fieldName = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);

      String localizedFieldMsg = messageService.getMessage(violation.getMessage());
      fieldErrors.put(fieldName, localizedFieldMsg);
    }

    ErrorCode errorCode = ErrorCode.VALIDATION_INVALID_INPUT;
    String localizedMessage = messageService.getMessage(errorCode.getMessage());

    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        fieldErrors
    );
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class})
  public ResponseEntity<ResponseGeneral<Object>> handleRequestBindingException(
      final Exception exception, HttpServletRequest request) {
    log.warn("(handleRequestBindingException) Invalid request parameter: {}",
        exception.getMessage());

    ErrorCode errorCode = ErrorCode.VALIDATION_INVALID_INPUT;
    String localizedMessage = messageService.getMessage(errorCode.getMessage());

    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        null
    );
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ResponseGeneral<Object>> handleHttpMessageNotReadableException(
      final HttpMessageNotReadableException e, HttpServletRequest request) {
    log.error("(handleHttpMessageNotReadableException) Request body is invalid: {}",
        e.getMessage());

    ErrorCode errorCode = ErrorCode.VALIDATION_INVALID_INPUT;
    String localizedMessage = messageService.getMessage(errorCode.getMessage());

    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        null
    );
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ResponseGeneral<Object>> handleMaxUploadSizeExceededException(
      final MaxUploadSizeExceededException e, HttpServletRequest request) {
    log.warn("(handleMaxUploadSizeExceededException) File size exceeds limit: {}", e.getMessage());

    ErrorCode errorCode = ErrorCode.ATTACHMENT_FILE_TOO_LARGE;
    String localizedMessage = messageService.getMessage(errorCode.getMessage());

    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        null
    );
    return new ResponseEntity<>(response, errorCode.getStatus());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ResponseGeneral<Object>> handleGlobalException(
      final Exception e, HttpServletRequest request) {
    log.error("(handleGlobalException) System error encountered:", e);

    ErrorCode errorCode = ErrorCode.SYSTEM_INTERNAL_ERROR;
    String localizedMessage = messageService.getMessage(errorCode.getMessage());

    ResponseGeneral<Object> response = ResponseGeneral.error(
        errorCode.getStatus().value(),
        errorCode.getCode(),
        localizedMessage,
        null
    );
    return new ResponseEntity<>(response, errorCode.getStatus());
  }
}
