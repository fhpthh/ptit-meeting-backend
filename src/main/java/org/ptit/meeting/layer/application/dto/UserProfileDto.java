package org.ptit.meeting.layer.application.dto;

import java.util.Set;
import lombok.Builder;
import org.ptit.meeting.layer.domain.enums.AccountType;

@Builder
public record UserProfileDto(
    Long id,
    String email,
    String code,
    String fullName,
    AccountType accountType,
    String department,
    Set<String> roles,
    Set<String> permissions,
    boolean mustChangePassword
) {

}
