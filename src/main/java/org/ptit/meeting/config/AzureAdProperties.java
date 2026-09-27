package org.ptit.meeting.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "azure.activedirectory")
public class AzureAdProperties {

  private String clientId;
  private String clientSecret;
  private String tenantId;
  private String redirectUri;
  private String tokenUrl;
  private String graphUserInfoUrl;

  public String buildAuthorizeUrl() {
    return String.format(
        "https://login.microsoftonline.com/%s/oauth2/v2.0/authorize?client_id=%s&response_type=code&redirect_uri=%s&response_mode=query&scope=openid%%20profile%%20email%%20User.Read&prompt=select_account",
        (tenantId != null && !tenantId.isBlank()) ? tenantId : "common",
        clientId,
        redirectUri
    );
  }
}
