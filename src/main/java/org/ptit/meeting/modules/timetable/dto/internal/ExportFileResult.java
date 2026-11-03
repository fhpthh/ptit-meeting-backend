package org.ptit.meeting.modules.timetable.dto.internal;

public record ExportFileResult(
    String fileName,
    byte[] content
) {
}
