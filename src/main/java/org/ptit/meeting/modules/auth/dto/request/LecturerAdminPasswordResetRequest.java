package org.ptit.meeting.modules.auth.dto.request;

public record LecturerAdminPasswordResetRequest(
    String temporaryPassword
) {}
