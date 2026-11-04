package org.ptit.meeting.modules.timetable.dto.response;

import java.time.LocalDateTime;
import org.ptit.meeting.modules.timetable.constant.enums.ImportStatus;
import org.ptit.meeting.modules.timetable.constant.enums.ImportType;

public record ImportHistoryResponse(
    Long id,
    Long semesterId,
    String semesterCode,
    String semesterName,
    Long uploadedBy,
    String uploadedByCode,
    String uploadedByName,
    String fileName,
    ImportType importType,
    int totalRows,
    int successRows,
    int failedRows,
    ImportStatus status,
    boolean hasErrors,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
