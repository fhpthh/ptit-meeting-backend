package org.ptit.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangePasswordFirstTimeRequest(
    @NotNull(message = "User ID không được để trống")
    Long userId,

    @NotBlank(message = "Mật khẩu cũ không được để trống")
    String oldPassword,

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 8, message = "Mật khẩu mới phải có tối thiểu 8 ký tự")
    String newPassword,

    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    String confirmNewPassword
) {}
