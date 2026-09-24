package org.ptit.meeting.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.ptit.meeting.common.constant.ErrorConstants;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.utils.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final MessageUtil messageUtil;

  public GlobalExceptionHandler(MessageUtil messageUtil) {
    this.messageUtil = messageUtil;
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<BaseResponse<Void>> handleBusinessException(BusinessException exception) {
    logException(exception);
    StatusCode statusCode = exception.getStatusCode();
    String message = messageUtil.getMessage(exception.getStrCode(), exception.getArgs());

    return ResponseEntity
        .status(statusCode.getHttpStatusCode())
        .body(BaseResponse.error(statusCode.getCode(), exception.getStrCode(), message));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<BaseResponse<Void>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception
  ) {
    logException(exception);
    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError error : exception.getBindingResult().getFieldErrors()) {
      errors.putIfAbsent(error.getField(), error.getDefaultMessage());
    }

    return ResponseEntity.badRequest().body(BaseResponse.error(
        StatusCode.INVALID_INPUT_DATA.getCode(),
        ErrorConstants.INVALID_INPUT,
        errors.toString()
    ));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<BaseResponse<Void>> handleConstraintViolation(
      ConstraintViolationException exception
  ) {
    logException(exception);
    return invalidInputResponse(exception.getMessage());
  }

  @ExceptionHandler({
      HttpMessageNotReadableException.class,
      MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<BaseResponse<Void>> handleArgumentTypeMismatch(Exception exception) {
    logException(exception);
    return invalidInputResponse(ErrorConstants.ARGUMENT_TYPE_MISMATCH);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<BaseResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
    logException(exception);
    return invalidInputResponse(ErrorConstants.INVALID_INPUT);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<BaseResponse<Void>> handleUnexpectedException(Exception exception) {
    logException(exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(BaseResponse.error(
        StatusCode.ERROR_IN_BACKEND.getCode(),
        ErrorConstants.SERVER_ERROR,
        messageUtil.getMessage(ErrorConstants.SERVER_ERROR)
    ));
  }

  private ResponseEntity<BaseResponse<Void>> invalidInputResponse(String strCode) {
    return ResponseEntity.badRequest().body(BaseResponse.error(
        StatusCode.INVALID_INPUT_DATA.getCode(),
        strCode,
        messageUtil.getMessage(strCode)
    ));
  }

  private void logException(Exception exception) {
    log.error("Exception handled: {}", exception.getMessage(), exception);
  }
}
