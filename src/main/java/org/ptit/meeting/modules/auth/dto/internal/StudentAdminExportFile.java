package org.ptit.meeting.modules.auth.dto.internal;

public record StudentAdminExportFile(String filename, String contentType, byte[] content) {}
