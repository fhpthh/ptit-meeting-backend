
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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;
  private final MessageUtil messageUtil;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException
  ) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    BaseResponse<Object> errorResponse = BaseResponse.error(
        StatusCode.AUTHENTICATION_FAILED.getCode(),
        AuthErrorConstants.UNAUTHORIZED,
        messageUtil.getMessage(AuthErrorConstants.UNAUTHORIZED),
        null
    );

    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}
