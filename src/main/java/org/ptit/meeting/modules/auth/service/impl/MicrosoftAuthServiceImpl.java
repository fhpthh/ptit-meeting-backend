
package org.ptit.meeting.modules.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.client.MicrosoftGraphClient;
import org.ptit.meeting.modules.auth.constant.AuthErrorConstants;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.service.MicrosoftAuthService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MicrosoftAuthServiceImpl implements MicrosoftAuthService {

  private static final String STUDENT_EMAIL_DOMAIN = "@stu.ptit.edu.vn";
  private static final String STAFF_EMAIL_DOMAIN = "@ptit.edu.vn";

  private final MicrosoftGraphClient microsoftGraphClient;

  @Override
  public MicrosoftGraphClient.MicrosoftUserProfile authenticateAndFetchProfile(String code, String redirectUri) {
    log.info("(authenticateAndFetchProfile) Exchanging code for redirectUri: {}", redirectUri);
    return microsoftGraphClient.authenticateAndFetchProfile(code, redirectUri);
  }

  @Override
  public AccountType resolveAccountType(String email) {
    if (email.endsWith(STUDENT_EMAIL_DOMAIN)) {
      return AccountType.STUDENT;
    }
    if (email.endsWith(STAFF_EMAIL_DOMAIN)) {
      return AccountType.STAFF;
    }
    log.warn("(resolveAccountType) Email domain not permitted: {}", email);
    throw new BusinessException(StatusCode.AUTHORIZATION_FAILED, AuthErrorConstants.INVALID_DOMAIN);
  }

  @Override
  public String extractStudentCode(String email, AccountType accountType) {
    if (AccountType.STUDENT.equals(accountType) && email.contains("@")) {
      String localPart = email.substring(0, email.indexOf('@')).toUpperCase();
      if (localPart.contains(".")) {
        return localPart.substring(localPart.lastIndexOf('.') + 1);
      }
      return localPart;
    }
    return null;
  }

  @Override
  public String resolveEmail(MicrosoftGraphClient.MicrosoftUserProfile profile) {
    String candidate = null;

    if (profile.email() != null && !profile.email().isBlank()) {
      candidate = profile.email().trim().toLowerCase();
    } else if (profile.userPrincipalName() != null && !profile.userPrincipalName().isBlank()) {
      candidate = profile.userPrincipalName().trim().toLowerCase();
    }

    if (candidate == null) {
      log.error("(resolveEmail) Both email and userPrincipalName are missing for oid: {}", profile.oid());
      throw new BusinessException(StatusCode.AUTHORIZATION_FAILED, AuthErrorConstants.INVALID_DOMAIN);
    }

    // Xử lý tài khoản khách mời / guest external account trên Azure AD
    // Định dạng Microsoft: username_domain#ext#@tenant.onmicrosoft.com hoặc username@domain#ext#@...
    if (candidate.contains("#ext#")) {
      String beforeExt = candidate.substring(0, candidate.indexOf("#ext#"));
      if (beforeExt.contains("@")) {
        candidate = beforeExt;
      } else {
        int lastUnderscore = beforeExt.lastIndexOf('_');
        if (lastUnderscore > 0) {
          candidate = beforeExt.substring(0, lastUnderscore) + "@" + beforeExt.substring(lastUnderscore + 1);
        }
      }
      log.info("(resolveEmail) Resolved Azure AD external guest account to original email: {}", candidate);
    }

    return candidate;
  }
}
