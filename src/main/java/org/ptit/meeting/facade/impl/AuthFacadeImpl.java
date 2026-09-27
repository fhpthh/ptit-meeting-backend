package org.ptit.meeting.facade.impl;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.client.MicrosoftGraphClient;
import org.ptit.meeting.constant.enums.AccountType;
import org.ptit.meeting.constant.enums.IdpProvider;
import org.ptit.meeting.dto.request.ChangePasswordFirstTimeRequest;
import org.ptit.meeting.dto.request.LoginRequest;
import org.ptit.meeting.dto.request.OutlookLoginRequest;
import org.ptit.meeting.dto.request.RefreshTokenRequest;
import org.ptit.meeting.dto.response.AuthResponse;
import org.ptit.meeting.dto.response.TokenPairDto;
import org.ptit.meeting.entity.User;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.exception.ErrorCode;
import org.ptit.meeting.facade.AuthFacade;
import org.ptit.meeting.security.JwtTokenProvider;
import org.ptit.meeting.service.IdentityProviderService;
import org.ptit.meeting.service.MicrosoftAuthService;
import org.ptit.meeting.service.RateLimitService;
import org.ptit.meeting.service.TokenService;
import org.ptit.meeting.service.UserService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFacadeImpl implements AuthFacade {

  private final UserService userService;
  private final IdentityProviderService identityProviderService;
  private final TokenService tokenService;
  private final RateLimitService rateLimitService;
  private final MicrosoftAuthService microsoftAuthService;
  private final JwtTokenProvider jwtTokenProvider;

  @Override
  @Transactional
  public AuthResponse login(LoginRequest request, String clientIp, String userAgent) {
    log.info("(login) username: {}, clientIp: {}", request.username(), clientIp);

    // 1. Kiểm tra phòng chống tấn công brute-force
    rateLimitService.checkRateLimit(request.username());

    // 2. Xác thực thông tin đăng nhập
    User user;
    try {
      user = userService.authenticateWithPassword(request.username(), request.password());
    } catch (Exception e) {
      rateLimitService.recordLoginFailure(request.username());
      throw e;
    }

    // Xóa bộ đếm đăng nhập sai khi thành công
    rateLimitService.resetLoginFailures(request.username());

    // 3. Kiểm tra trạng thái tài khoản
    userService.validateUserStatus(user);

    // 4. Kiểm tra bắt buộc đổi mật khẩu lần đầu
    if (user.isMustChangePassword()) {
      log.warn("(login) User must change password: {}", user.getCode());
      TokenPairDto tempToken = tokenService.generateToken(user, UUID.randomUUID().toString());
      return buildAuthResponse(user, tempToken, true);
    }

    // 5. Cấp token phiên làm việc chính thức
    TokenPairDto tokenPair = tokenService.createSession(user, clientIp, userAgent);
    log.info("(login) Login successful for user: {}", user.getCode());

    return buildAuthResponse(user, tokenPair, false);
  }

  @Override
  @Transactional
  public AuthResponse loginWithOutlook(OutlookLoginRequest request, String clientIp, String userAgent) {
    log.info("(loginWithOutlook) Exchanging code for redirectUrl: {}", request.redirectUrl());

    // 1. Đổi code lấy hồ sơ từ Microsoft Graph
    MicrosoftGraphClient.MicrosoftUserProfile msProfile =
        microsoftAuthService.authenticateAndFetchProfile(request.authorizationCode(), request.redirectUrl());

    String email = microsoftAuthService.resolveEmail(msProfile);
    AccountType accountType = microsoftAuthService.resolveAccountType(email);
    String studentCode = microsoftAuthService.extractStudentCode(email, accountType);

    log.info("(loginWithOutlook) Microsoft profile: oid={}, email={}, accountType={}",
        msProfile.oid(), email, accountType);

    // 2. Hợp nhất định danh hoặc JIT Provisioning
    User user = identityProviderService.findUserIdByProviderAndSubject(IdpProvider.AZURE_AD, msProfile.oid())
        .map(userId -> {
          identityProviderService.updateLastLogin(IdpProvider.AZURE_AD, msProfile.oid());
          User existing = userService.getUserById(userId);
          return userService.syncUserProfile(existing, msProfile.displayName(), studentCode);
        })
        .orElseGet(() -> {
          User targetUser = userService.findOrCreateByEmail(email, msProfile.displayName(), studentCode, accountType);
          identityProviderService.createLink(targetUser.getId(), IdpProvider.AZURE_AD, msProfile.oid(), email);
          return targetUser;
        });

    // 3. Kiểm tra trạng thái tài khoản
    userService.validateUserStatus(user);

    // 4. Cấp token phiên làm việc
    TokenPairDto tokenPair = tokenService.createSession(user, clientIp, userAgent);
    log.info("(loginWithOutlook) SSO login successful for userId: {}, email: {}", user.getId(), user.getEmail());

    return buildAuthResponse(user, tokenPair, false);
  }

  @Override
  @Transactional
  public AuthResponse changePasswordFirstTime(ChangePasswordFirstTimeRequest request, String clientIp, String userAgent) {
    log.info("(changePasswordFirstTime) userId: {}", request.userId());

    User user = userService.changePassword(request.userId(), request.oldPassword(), request.newPassword());

    TokenPairDto tokenPair = tokenService.createSession(user, clientIp, userAgent);
    log.info("(changePasswordFirstTime) Password changed successfully for userId: {}", user.getId());

    return buildAuthResponse(user, tokenPair, false);
  }

  @Override
  @Transactional(readOnly = true)
  public AuthResponse.UserSummary getCurrentUserProfile(Long userId) {
    log.info("(getCurrentUserProfile) userId: {}", userId);

    User user = userService.getUserById(userId);
    return mapToUserSummary(user);
  }

  @Override
  @Transactional
  public AuthResponse refreshToken(RefreshTokenRequest request, String clientIp, String userAgent) {
    log.info("(refreshToken) Refreshing session");

    Long userId = tokenService.getUserIdByRefreshToken(request.refreshToken());
    if (userId == null) {
      log.warn("(refreshToken) Invalid or expired refresh token: {}", request.refreshToken());
      throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED);
    }

    // Thu hồi refresh token cũ (Token rotation)
    tokenService.revokeRefreshToken(request.refreshToken());

    User user = userService.getUserById(userId);
    userService.validateUserStatus(user);

    TokenPairDto tokenPair = tokenService.createSession(user, clientIp, userAgent);
    log.info("(refreshToken) Session refreshed successfully for userId: {}", userId);

    return buildAuthResponse(user, tokenPair, false);
  }

  @Override
  public void logout(String bearerToken, String refreshToken) {
    log.info("(logout)");

    // 1. Blacklist Access Token nếu còn hạn
    if (bearerToken != null && !bearerToken.isBlank()) {
      try {
        String jwt = bearerToken.startsWith("Bearer ") ? bearerToken.substring(7) : bearerToken;
        if (jwtTokenProvider.validateToken(jwt)) {
          String jwtId = jwtTokenProvider.extractJwtId(jwt);
          long remainingTtl = jwtTokenProvider.getRemainingTtlSeconds(jwt);
          if (remainingTtl > 0) {
            tokenService.blacklistToken(jwtId, remainingTtl);
          }
        }
      } catch (Exception e) {
        log.warn("(logout) Could not blacklist access token: {}", e.getMessage());
      }
    }

    // 2. Thu hồi Refresh Token khỏi Redis
    if (refreshToken != null && !refreshToken.isBlank()) {
      tokenService.revokeRefreshToken(refreshToken);
    }
  }

  private AuthResponse buildAuthResponse(User user, TokenPairDto tokenPair, boolean mustChangePassword) {
    return AuthResponse.builder()
        .accessToken(tokenPair != null ? tokenPair.accessToken() : null)
        .refreshToken(tokenPair != null ? tokenPair.refreshToken() : null)
        .tokenType(tokenPair != null ? tokenPair.tokenType() : "Bearer")
        .expiresIn(tokenPair != null ? tokenPair.expiresIn() : 0)
        .mustChangePassword(mustChangePassword)
        .user(mapToUserSummary(user))
        .build();
  }

  private AuthResponse.UserSummary mapToUserSummary(User user) {
    return AuthResponse.UserSummary.builder()
        .id(user.getId())
        .email(user.getEmail())
        .code(user.getCode())
        .fullName(user.getFullName())
        .accountType(user.getAccountType())
        .department(user.getDepartment())
        .roles(user.getRoles())
        .permissions(user.getPermissions())
        .build();
  }
}
