
package org.ptit.meeting.modules.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.modules.auth.service.TokenService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String HEADER_AUTHORIZATION = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtTokenProvider jwtTokenProvider;
  private final TokenService tokenService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    try {
      String jwt = extractJwtFromRequest(request);

      if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
        String jwtId = jwtTokenProvider.extractJwtId(jwt);

        // Kiểm tra xem token có nằm trong Blacklist của Redis không
        if (tokenService.isTokenBlacklisted(jwtId)) {
          log.warn("(doFilterInternal) Token is blacklisted: jwtId={}", jwtId);
        } else {
          // Trích xuất trực tiếp CustomUserDetails từ JWT Claims (0 query Database)
          CustomUserDetails userDetails = jwtTokenProvider.extractUserDetails(jwt);

          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

          SecurityContextHolder.getContext().setAuthentication(authentication);
        }
      }
    } catch (Exception e) {
      log.error("(doFilterInternal) Cannot set user authentication: {}", e.getMessage());
      SecurityContextHolder.clearContext();
    }

    filterChain.doFilter(request, response);
  }

  private String extractJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader(HEADER_AUTHORIZATION);
    if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
      return bearerToken.substring(BEARER_PREFIX.length());
    }
    return null;
  }
}
