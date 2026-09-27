package org.ptit.meeting.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.dto.response.ResponseGeneral;
import org.ptit.meeting.util.CommonConstants;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException
  ) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    ResponseGeneral<Object> errorResponse = ResponseGeneral.error(
        CommonConstants.STATUS_UNAUTHORIZED,
        "AUTH_UNAUTHORIZED",
        "Yêu cầu xác thực. Vui lòng đăng nhập để tiếp tục.",
        null
    );

    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}
