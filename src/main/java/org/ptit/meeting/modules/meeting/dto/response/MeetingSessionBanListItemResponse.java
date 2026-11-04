package org.ptit.meeting.modules.meeting.dto.response;

import java.time.Instant;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;

public record MeetingSessionBanListItemResponse(
    String banId,
    Long userId,
    String email,
    String code,
    String fullName,
    AccountType accountType,
    String department,
    Long bannedByUserId,
    String reason,
    Instant bannedAt
) {
}
