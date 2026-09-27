package org.ptit.meeting.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.config.AzureAdProperties;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.exception.ErrorCode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class MicrosoftGraphClient {

  private final AzureAdProperties azureAdProperties;
  private final RestClient restClient = RestClient.create();
  private final ObjectMapper objectMapper = new ObjectMapper();

  public record MicrosoftUserProfile(
      String oid,
      String email,
      String displayName,
      String userPrincipalName
  ) {}

  public MicrosoftUserProfile authenticateAndFetchProfile(String code, String redirectUri) {
    log.info("(authenticateAndFetchProfile) Exchanging code with Microsoft for redirectUri: {}", redirectUri);

    try {
      // 1. Exchange authorization code lấy access_token và id_token từ Microsoft
      MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
      formData.add("client_id", azureAdProperties.getClientId());
      formData.add("client_secret", azureAdProperties.getClientSecret());
      formData.add("grant_type", "authorization_code");
      formData.add("code", code);
      formData.add("redirect_uri", redirectUri);
      formData.add("scope", "openid profile email User.Read");

      Map<String, Object> tokenResponse = restClient.post()
          .uri(azureAdProperties.getTokenUrl())
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(formData)
          .retrieve()
          .body(new ParameterizedTypeReference<>() {});

      if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
        log.error("(authenticateAndFetchProfile) Failed to exchange code for access token. Response: {}", tokenResponse);
        throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
      }

      String msAccessToken = (String) tokenResponse.get("access_token");
      String idToken = (String) tokenResponse.get("id_token");
      Map<String, Object> idTokenClaims = decodeJwtPayload(idToken);
      log.debug("(authenticateAndFetchProfile) Decoded id_token claims: {}", idTokenClaims.keySet());

      // 2. Gọi Microsoft Graph API /v1.0/me để lấy hồ sơ người dùng
      Map<String, Object> userProfileResponse = Collections.emptyMap();
      try {
        userProfileResponse = restClient.get()
            .uri(azureAdProperties.getGraphUserInfoUrl())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + msAccessToken)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
      } catch (Exception e) {
        log.warn("(authenticateAndFetchProfile) Graph API /v1.0/me request failed: {}. Will use ID token claims as fallback.", e.getMessage());
      }

      if (userProfileResponse == null) {
        userProfileResponse = Collections.emptyMap();
      }

      // 3. Fallback đa tầng (Multi-tier fallback chain)
      // OID (Microsoft Object ID)
      String oid = (String) userProfileResponse.get("id");
      if (oid == null || oid.isBlank()) {
        oid = (String) idTokenClaims.get("oid");
      }
      if (oid == null || oid.isBlank()) {
        oid = (String) idTokenClaims.get("sub");
      }

      // Email
      String email = (String) userProfileResponse.get("mail");
      if ((email == null || email.isBlank()) && userProfileResponse.get("otherMails") instanceof java.util.List<?> otherMails && !otherMails.isEmpty()) {
        email = otherMails.get(0).toString();
      }
      if (email == null || email.isBlank()) {
        email = (String) idTokenClaims.get("preferred_username");
      }
      if (email == null || email.isBlank()) {
        email = (String) idTokenClaims.get("email");
      }

      String userPrincipalName = (String) userProfileResponse.get("userPrincipalName");
      if (userPrincipalName == null || userPrincipalName.isBlank()) {
        userPrincipalName = (String) idTokenClaims.get("preferred_username");
      }
      if (email == null || email.isBlank()) {
        email = userPrincipalName;
      }

      // Display Name (Họ tên)
      String graphDisplayName = (String) userProfileResponse.get("displayName");
      String idTokenName = (String) idTokenClaims.get("name");
      String displayName = resolveBestDisplayName(graphDisplayName, idTokenName, email);

      log.info("(authenticateAndFetchProfile) Resolved Microsoft profile: oid={}, email={}, displayName={}, upn={}",
          oid, email, displayName, userPrincipalName);

      if (oid == null || email == null) {
        log.error("(authenticateAndFetchProfile) Incomplete profile from Microsoft: oid={}, email={}", oid, email);
        throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
      }

      return new MicrosoftUserProfile(oid, email, displayName, userPrincipalName);
    } catch (org.springframework.web.client.RestClientResponseException e) {
      log.error("(authenticateAndFetchProfile) Microsoft OAuth API error: status={}, body={}",
          e.getStatusCode(), e.getResponseBodyAsString());
      throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
    } catch (BusinessException be) {
      throw be;
    } catch (Exception e) {
      log.error("(authenticateAndFetchProfile) Error connecting to Microsoft OAuth API: {}", e.getMessage(), e);
      throw new BusinessException(ErrorCode.SYSTEM_INTERNAL_ERROR);
    }
  }

  private Map<String, Object> decodeJwtPayload(String jwtToken) {
    if (jwtToken == null || !jwtToken.contains(".")) {
      return Collections.emptyMap();
    }
    try {
      String[] parts = jwtToken.split("\\.");
      if (parts.length >= 2) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
        return objectMapper.readValue(decodedBytes, new TypeReference<Map<String, Object>>() {});
      }
    } catch (Exception e) {
      log.warn("(decodeJwtPayload) Could not decode ID token payload: {}", e.getMessage());
    }
    return Collections.emptyMap();
  }

  private String resolveBestDisplayName(String graphName, String idTokenName, String email) {
    if (isMeaningfulFullName(graphName) && !isMeaningfulFullName(idTokenName)) {
      return graphName.trim();
    }
    if (isMeaningfulFullName(idTokenName) && !isMeaningfulFullName(graphName)) {
      return idTokenName.trim();
    }
    if (graphName != null && !graphName.isBlank()) {
      return graphName.trim();
    }
    if (idTokenName != null && !idTokenName.isBlank()) {
      return idTokenName.trim();
    }
    if (email != null && email.contains("@")) {
      return email.substring(0, email.indexOf('@'));
    }
    return "PTIT User";
  }

  private boolean isMeaningfulFullName(String name) {
    if (name == null || name.isBlank()) {
      return false;
    }
    String trimmed = name.trim();
    return trimmed.contains(" ");
  }
}
