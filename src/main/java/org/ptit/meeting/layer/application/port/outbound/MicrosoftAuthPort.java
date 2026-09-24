package org.ptit.meeting.layer.application.port.outbound;

public interface MicrosoftAuthPort {

  MicrosoftUserProfile authenticateAndFetchProfile(String code, String redirectUri);

  record MicrosoftUserProfile(
      String oid,
      String email,
      String displayName,
      String userPrincipalName
  ) {

  }
}
