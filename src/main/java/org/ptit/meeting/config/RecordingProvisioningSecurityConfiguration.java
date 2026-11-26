package org.ptit.meeting.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.ai.constant.AiErrorConstants;
import org.ptit.meeting.modules.ai.dto.internal.RecordingTrustedWorker;
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
public class RecordingProvisioningSecurityConfiguration {
  @Bean
  @Order(0)
  public SecurityFilterChain recordingProvisioningSecurity(
      HttpSecurity http,
      AiRecordingProperties properties,
      ObjectMapper mapper,
      MessageUtil messages)
      throws Exception {
    var config = properties.getProvisioning();
    boolean enabled = properties.isEnabled() && config.isEnabled();
    if (config.isEnabled()
        && (!enabled
            || config.getWorkerId() == null
            || !config.getWorkerId().matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
            || config.getServiceSecret() == null
            || !config.getServiceSecret().matches("[a-fA-F0-9]{64}")
            || config.getServiceSecret().equals(properties.getWorkerSecret()))) {
      throw new IllegalStateException("Invalid recording worker provisioning configuration");
    }
    http.securityMatcher("/internal/v1/recording-credentials/**")
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a.anyRequest().hasRole("RECORDING_PROVISIONER"))
        .addFilterBefore(
            new OncePerRequestFilter() {
              @Override
              protected void doFilterInternal(
                  HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                  throws ServletException, IOException {
                response.setHeader("Cache-Control", "no-store");
                String header = request.getHeader("Authorization");
                boolean valid =
                    enabled
                        && config.getWorkerId().equals(request.getHeader("X-Recording-Worker"))
                        && header != null
                        && header.startsWith("Bearer ")
                        && MessageDigest.isEqual(
                            config.getServiceSecret().getBytes(StandardCharsets.UTF_8),
                            header.substring(7).getBytes(StandardCharsets.UTF_8));
                if (!valid) {
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
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        new RecordingTrustedWorker(config.getWorkerId()),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_RECORDING_PROVISIONER"))));
                SecurityContextHolder.setContext(context);
                chain.doFilter(request, response);
              }
            },
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
