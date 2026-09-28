
package org.ptit.meeting.modules.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.config.JwtProperties;
import org.ptit.meeting.modules.auth.dto.response.TokenPairDto;
import org.ptit.meeting.modules.auth.entity.User;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

  private final JwtProperties jwtProperties;
  private final SecretKey key;

  public JwtTokenProvider(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    this.key = Keys.hmacShaKeyFor(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8));
  }

  public TokenPairDto generateToken(User user, String tokenId) {
    Date now = new Date();
    Date accessExpiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpirationMs());

    Map<String, Object> claims = new HashMap<>();
    claims.put("email", user.getEmail());
    claims.put("code", user.getCode());
    claims.put("accountType", user.getAccountType() != null ? user.getAccountType().name() : null);
    claims.put("roles", user.getRoles());
    claims.put("permissions", user.getPermissions());
    claims.put("mustChangePassword", user.isMustChangePassword());

    String accessToken = Jwts.builder()
        .id(tokenId)
        .subject(String.valueOf(user.getId()))
        .claims(claims)
        .issuedAt(now)
        .expiration(accessExpiry)
        .signWith(key, Jwts.SIG.HS256)
        .compact();

    return TokenPairDto.builder()
        .accessToken(accessToken)
        .refreshToken(tokenId)
        .tokenType("Bearer")
        .expiresIn(jwtProperties.getAccessTokenExpirationMs() / 1000)
        .build();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.error("(validateToken) Invalid JWT token: {}", e.getMessage());
      return false;
    }
  }

  public Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public Long extractUserId(String token) {
    return Long.parseLong(extractAllClaims(token).getSubject());
  }

  public String extractJwtId(String token) {
    return extractAllClaims(token).getId();
  }

  @SuppressWarnings("unchecked")
  public CustomUserDetails extractUserDetails(String token) {
    Claims claims = extractAllClaims(token);
    Long userId = Long.parseLong(claims.getSubject());
    String email = claims.get("email", String.class);
    String code = claims.get("code", String.class);
    List<String> roles = claims.get("roles", List.class);
    List<String> permissions = claims.get("permissions", List.class);

    return new CustomUserDetails(
        userId,
        email != null ? email : "",
        code != null ? code : "",
        null,
        roles,
        permissions,
        true
    );
  }

  public long getRemainingTtlSeconds(String token) {
    try {
      Date expiration = extractAllClaims(token).getExpiration();
      long remainingMs = expiration.getTime() - System.currentTimeMillis();
      return Math.max(0, remainingMs / 1000);
    } catch (Exception e) {
      log.warn("(getRemainingTtlSeconds) Cannot extract expiration: {}", e.getMessage());
      return 0;
    }
  }
}
