package org.ptit.meeting.layer.application.dto.command;

public record ChangePasswordFirstTimeCommand(
    Long userId,
    String oldPassword,
    String newPassword,
    String clientIp,
    String userAgent
) {

}
