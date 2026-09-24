package org.ptit.meeting.layer.application.dto.command;

public record LoginCommand(
    String username,
    String password,
    String clientIp,
    String userAgent
) {

}
