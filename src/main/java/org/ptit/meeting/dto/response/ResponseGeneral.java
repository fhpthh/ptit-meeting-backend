package org.ptit.meeting.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.ptit.meeting.util.CommonConstants;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseGeneral<T> {

  private int status;
  private String message;
  private T data;
  private Instant timestamp;
  private ApiError apiError;

  public static <T> ResponseGeneral<T> success(String message, T data) {
    return ResponseGeneral.<T>builder()
        .status(CommonConstants.STATUS_OK)
        .message(message)
        .data(data)
        .timestamp(Instant.now())
        .build();
  }

  public static <T> ResponseGeneral<T> success(T data) {
    return success("Thao tác thành công", data);
  }

  public static <T> ResponseGeneral<T> error(int status, String errorCode, String message, Object details) {
    return ResponseGeneral.<T>builder()
        .status(status)
        .message(message)
        .timestamp(Instant.now())
        .apiError(ApiError.of(errorCode, message, details))
        .build();
  }
}
