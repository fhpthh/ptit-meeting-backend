
package org.ptit.meeting.modules.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.common.constant.ErrorConstants;
import org.ptit.meeting.common.constant.StatusCode;
import org.ptit.meeting.config.AzureAdProperties;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.constant.AuthErrorConstants;
import org.ptit.meeting.modules.auth.dto.request.ChangePasswordFirstTimeRequest;
import org.ptit.meeting.modules.auth.dto.request.LoginRequest;
import org.ptit.meeting.modules.auth.dto.request.LogoutRequest;
import org.ptit.meeting.modules.auth.dto.request.OutlookLoginRequest;
import org.ptit.meeting.modules.auth.dto.request.RefreshTokenRequest;
import org.ptit.meeting.modules.auth.dto.response.AuthResponse;
import org.ptit.meeting.modules.auth.facade.AuthFacade;
import org.ptit.meeting.modules.auth.security.CustomUserDetails;
import org.ptit.meeting.modules.auth.service.MessageService;
import org.ptit.meeting.modules.auth.util.MessageKeyConstant;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthFacade authFacade;
  private final MessageService messageService;
  private final AzureAdProperties azureAdProperties;

  /**
   * Đăng nhập thông thường bằng Mã sinh viên hoặc Email và Mật khẩu.
   */
  @PostMapping("/login")
  public BaseResponse<AuthResponse> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest
  ) {
    log.info("(login) request: {}", request);
    ClientMetadata metadata = extractClientMetadata(httpRequest);
    AuthResponse response = authFacade.login(request, metadata.ip(), metadata.userAgent());

    String messageKey = response.mustChangePassword()
        ? MessageKeyConstant.AUTH_MUST_CHANGE_PASSWORD
        : MessageKeyConstant.AUTH_LOGIN_SUCCESS;

    return BaseResponse.success(messageService.getMessage(messageKey), response);
  }

  /**
   * Đăng nhập một chạm qua Microsoft Outlook (SSO Microsoft Entra ID).
   */
  @PostMapping("/outlook")
  public BaseResponse<AuthResponse> loginWithOutlook(
      @Valid @RequestBody OutlookLoginRequest request,
      HttpServletRequest httpRequest
  ) {
    log.info("(loginWithOutlook) request: {}", request);
    ClientMetadata metadata = extractClientMetadata(httpRequest);
    AuthResponse response = authFacade.loginWithOutlook(request, metadata.ip(), metadata.userAgent());

    return BaseResponse.success(
        messageService.getMessage(MessageKeyConstant.AUTH_OUTLOOK_LOGIN_SUCCESS),
        response
    );
  }

  /**
   * Đổi mật khẩu bắt buộc ở lần đầu đăng nhập.
   */
  @PostMapping("/change-password-first-time")
  public BaseResponse<AuthResponse> changePasswordFirstTime(
      @Valid @RequestBody ChangePasswordFirstTimeRequest request,
      HttpServletRequest httpRequest
  ) {
    log.info("(changePasswordFirstTime) request: {}", request);

    if (!request.newPassword().equals(request.confirmNewPassword())) {
      throw BusinessException.invalidInput(ErrorConstants.INVALID_INPUT);
    }

    ClientMetadata metadata = extractClientMetadata(httpRequest);
    AuthResponse response = authFacade.changePasswordFirstTime(request, metadata.ip(), metadata.userAgent());

    return BaseResponse.success(
        messageService.getMessage(MessageKeyConstant.AUTH_CHANGE_PASSWORD_SUCCESS),
        response
    );
  }

  /**
   * Lấy đường link URL đăng nhập Microsoft Entra ID (Dành cho Frontend / Postman bấm vào đăng nhập).
   */
  @GetMapping("/outlook/authorize-url")
  public BaseResponse<Map<String, String>> getOutlookAuthorizeUrl() {
    log.info("(getOutlookAuthorizeUrl)");
    String url = azureAdProperties.buildAuthorizeUrl();
    Map<String, String> response = Map.of("authorizeUrl", url);

    return BaseResponse.success(
        messageService.getMessage(MessageKeyConstant.AUTH_OUTLOOK_URL_SUCCESS),
        response
    );
  }

  /**
   * Callback nhận mã ủy quyền từ Microsoft sau khi đăng nhập trên trình duyệt thành công.
   */
  @GetMapping("/outlook/callback")
  public BaseResponse<AuthResponse> handleOutlookCallback(
      @RequestParam("code") String code,
      HttpServletRequest httpRequest
  ) {
    log.info("(handleOutlookCallback) code: {}", code);
    OutlookLoginRequest request = new OutlookLoginRequest(code, azureAdProperties.getRedirectUri());
    ClientMetadata metadata = extractClientMetadata(httpRequest);
    AuthResponse response = authFacade.loginWithOutlook(request, metadata.ip(), metadata.userAgent());

    return BaseResponse.success(
        messageService.getMessage(MessageKeyConstant.AUTH_OUTLOOK_LOGIN_SUCCESS),
        response
    );
  }

  /**
   * Lấy thông tin hồ sơ của người dùng đang đăng nhập qua Token JWT (Bearer Token).
   */
  @GetMapping("/me")
  public BaseResponse<AuthResponse.UserSummary> getCurrentUserProfile(
      @AuthenticationPrincipal CustomUserDetails userDetails
  ) {
    log.info("(getCurrentUserProfile) userDetails: {}", userDetails != null ? userDetails.getUserId() : null);

    if (userDetails == null) {
      throw new BusinessException(StatusCode.AUTHENTICATION_FAILED, AuthErrorConstants.UNAUTHORIZED);
    }

    AuthResponse.UserSummary response = authFacade.getCurrentUserProfile(userDetails.getUserId());

    return BaseResponse.success(
        messageService.getMessage(MessageKeyConstant.AUTH_GET_PROFILE_SUCCESS),
        response
    );
  }

  /**
   * Làm mới phiên đăng nhập (Refresh Token).
   */
  @PostMapping("/refresh")
  public BaseResponse<AuthResponse> refreshToken(
      @Valid @RequestBody RefreshTokenRequest request,
      HttpServletRequest httpRequest
  ) {
    log.info("(refreshToken)");
    ClientMetadata metadata = extractClientMetadata(httpRequest);
    AuthResponse response = authFacade.refreshToken(request, metadata.ip(), metadata.userAgent());

    return BaseResponse.success(
        messageService.getMessage(MessageKeyConstant.AUTH_REFRESH_TOKEN_SUCCESS),
        response
    );
  }

  /**
   * Đăng xuất an toàn: Thu hồi Refresh Token và Blacklist Access Token trên Redis.
   */
  @PostMapping("/logout")
  public BaseResponse<Void> logout(
      @RequestBody(required = false) LogoutRequest request,
      HttpServletRequest httpRequest
  ) {
    log.info("(logout)");
    String authorizationHeader = httpRequest.getHeader("Authorization");
    String refreshToken = request != null ? request.refreshToken() : null;

    authFacade.logout(authorizationHeader, refreshToken);

    return BaseResponse.success(
        messageService.getMessage(MessageKeyConstant.AUTH_LOGOUT_SUCCESS),
        null
    );
  }

  private record ClientMetadata(String ip, String userAgent) {}

  private ClientMetadata extractClientMetadata(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    String ip = (xForwardedFor != null && !xForwardedFor.isBlank())
        ? xForwardedFor.split(",")[0].trim()
        : request.getRemoteAddr();
    return new ClientMetadata(ip, request.getHeader("User-Agent"));
  }
}
