
package org.ptit.meeting.modules.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.auth.constant.AuthErrorConstants;
import org.ptit.meeting.utils.MessageUtil;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;
  private final MessageUtil messageUtil;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException
  ) throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    BaseResponse<Object> errorResponse = BaseResponse.error(
        StatusCode.AUTHORIZATION_FAILED.getCode(),
        AuthErrorConstants.FORBIDDEN,
        messageUtil.getMessage(AuthErrorConstants.FORBIDDEN),
        null
    );

    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}
