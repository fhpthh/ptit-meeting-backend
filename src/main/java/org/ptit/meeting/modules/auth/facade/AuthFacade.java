
package org.ptit.meeting.modules.auth.facade;

import org.ptit.meeting.modules.auth.dto.request.ChangePasswordFirstTimeRequest;
import org.ptit.meeting.modules.auth.dto.request.LoginRequest;
import org.ptit.meeting.modules.auth.dto.request.OutlookLoginRequest;
import org.ptit.meeting.modules.auth.dto.request.RefreshTokenRequest;
import org.ptit.meeting.modules.auth.dto.response.AuthResponse;

public interface AuthFacade {

  AuthResponse login(LoginRequest request, String clientIp, String userAgent);

  AuthResponse loginWithOutlook(OutlookLoginRequest request, String clientIp, String userAgent);

  AuthResponse changePasswordFirstTime(ChangePasswordFirstTimeRequest request, String clientIp, String userAgent);

  AuthResponse.UserSummary getCurrentUserProfile(Long userId);

  AuthResponse refreshToken(RefreshTokenRequest request, String clientIp, String userAgent);

  void logout(String bearerToken, String refreshToken);
}
