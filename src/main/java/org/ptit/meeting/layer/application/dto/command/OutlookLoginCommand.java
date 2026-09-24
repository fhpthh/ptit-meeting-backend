package org.ptit.meeting.layer.application.dto.command;

public record OutlookLoginCommand (
    String code,
    String redirectUrl,
    String clientIp,
    String userAgent
) {
}
