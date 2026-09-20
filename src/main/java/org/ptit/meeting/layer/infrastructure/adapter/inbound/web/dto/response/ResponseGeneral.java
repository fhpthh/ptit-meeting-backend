package org.ptit.meeting.layer.infrastructure.adapter.inbound.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import org.ptit.meeting.util.CommonConstants;
import org.ptit.meeting.util.MessageKeyConstant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseGeneral<T>(
    int status,
    String message,
    T data,
    Instant timestamp,
    ApiError apiError
) {

  public static <T> ResponseGeneral<T> of(int status, String message, T data, ApiError apiError) {
    return new ResponseGeneral<>(status, message, data, Instant.now(), apiError);
  }

  public static <T> ResponseGeneral<T> success(String message, T data) {
    return of(CommonConstants.STATUS_OK, message, data, null);
  }

  public static <T> ResponseGeneral<T> success(T data) {
    return of(CommonConstants.STATUS_OK, MessageKeyConstant.COMMON_SUCCESS, data, null);
  }

  public static <T> ResponseGeneral<T> created(String message, T data) {
    return of(CommonConstants.STATUS_CREATED, message, data, null);
  }

  public static <T> ResponseGeneral<T> error(
      int status, String code, String message, Object fieldError) {
    ApiError apiError = ApiError.builder()
        .code(code)
        .message(message)
        .fieldErrors(fieldError)
        .build();
    return of(status, message, null, apiError);
  }
}
