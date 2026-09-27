package org.ptit.meeting.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.dto.response.ResponseGeneral;
import org.ptit.meeting.util.CommonConstants;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper objectMapper;

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException
  ) throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");

    ResponseGeneral<Object> errorResponse = ResponseGeneral.error(
        CommonConstants.STATUS_FORBIDDEN,
        "AUTH_FORBIDDEN",
        "Bạn không có quyền thực hiện hành động này.",
        null
    );

    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
  }
}
