package org.ptit.meeting.facade;

import org.ptit.meeting.dto.request.ChangePasswordFirstTimeRequest;
import org.ptit.meeting.dto.request.LoginRequest;
import org.ptit.meeting.dto.request.OutlookLoginRequest;
import org.ptit.meeting.dto.request.RefreshTokenRequest;
import org.ptit.meeting.dto.response.AuthResponse;

public interface AuthFacade {

  AuthResponse login(LoginRequest request, String clientIp, String userAgent);

  AuthResponse loginWithOutlook(OutlookLoginRequest request, String clientIp, String userAgent);

  AuthResponse changePasswordFirstTime(ChangePasswordFirstTimeRequest request, String clientIp, String userAgent);

  AuthResponse.UserSummary getCurrentUserProfile(Long userId);

  AuthResponse refreshToken(RefreshTokenRequest request, String clientIp, String userAgent);

  void logout(String bearerToken, String refreshToken);
}
