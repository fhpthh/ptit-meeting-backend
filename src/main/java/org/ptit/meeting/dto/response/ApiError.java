package org.ptit.meeting.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

  private String code;
  private String message;
  private Object details;

  public static ApiError of(String code, String message, Object details) {
    return ApiError.builder()
        .code(code)
        .message(message)
        .details(details)
        .build();
  }
}
