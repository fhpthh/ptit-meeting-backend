package org.ptit.meeting.service;

import org.ptit.meeting.client.MicrosoftGraphClient;
import org.ptit.meeting.constant.enums.AccountType;

public interface MicrosoftAuthService {

  MicrosoftGraphClient.MicrosoftUserProfile authenticateAndFetchProfile(String code, String redirectUri);

  AccountType resolveAccountType(String email);

  String extractStudentCode(String email, AccountType accountType);

  String resolveEmail(MicrosoftGraphClient.MicrosoftUserProfile profile);
}
