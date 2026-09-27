
package org.ptit.meeting.modules.auth.service;

import org.ptit.meeting.modules.auth.client.MicrosoftGraphClient;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;

public interface MicrosoftAuthService {

  MicrosoftGraphClient.MicrosoftUserProfile authenticateAndFetchProfile(String code, String redirectUri);

  AccountType resolveAccountType(String email);

  String extractStudentCode(String email, AccountType accountType);

  String resolveEmail(MicrosoftGraphClient.MicrosoftUserProfile profile);
}
