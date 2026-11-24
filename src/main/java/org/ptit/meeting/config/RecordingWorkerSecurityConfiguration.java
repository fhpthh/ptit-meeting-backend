package org.ptit.meeting.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.ai.constant.AiErrorConstants;
import org.ptit.meeting.modules.ai.service.RecordingWorkerTokenService;
import org.ptit.meeting.utils.MessageUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class RecordingWorkerSecurityConfiguration {
  @Bean
  @Order(1)
  SecurityFilterChain recordingWorkerSecurity(
      HttpSecurity http,
      RecordingWorkerTokenService tokens,
      ObjectMapper mapper,
      MessageUtil messages)
      throws Exception {
    http.securityMatcher("/internal/v1/recording-jobs/**")
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("RECORDING_WORKER"))
        .addFilterBefore(
            new OncePerRequestFilter() {
              @Override
              protected void doFilterInternal(
                  HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                  throws ServletException, IOException {
                response.setHeader("Cache-Control", "no-store");
                try {
                  String header = request.getHeader("Authorization");
                  if (header == null || !header.startsWith("Bearer "))
                    throw new IllegalArgumentException();
                  var identity = tokens.verify(header.substring(7));
                  var context = SecurityContextHolder.createEmptyContext();
                  context.setAuthentication(
                      new UsernamePasswordAuthenticationToken(
                          identity,
                          null,
                          List.of(new SimpleGrantedAuthority("ROLE_RECORDING_WORKER"))));
                  SecurityContextHolder.setContext(context);
                } catch (BusinessException | IllegalArgumentException exception) {
                  SecurityContextHolder.clearContext();
                  response.setStatus(401);
                  response.setContentType("application/json");
                  response.setCharacterEncoding("UTF-8");
                  mapper.writeValue(
                      response.getWriter(),
                      BaseResponse.error(
                          StatusCode.AUTHENTICATION_FAILED.getCode(),
                          AiErrorConstants.RECORDING_WORKER_TOKEN,
                          messages.getMessage(AiErrorConstants.RECORDING_WORKER_TOKEN)));
                  return;
                }
                chain.doFilter(request, response);
              }
            },
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
