package org.ptit.meeting.modules.timetable.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import org.ptit.meeting.modules.timetable.constant.enums.ImportStatus;
import org.ptit.meeting.modules.timetable.constant.enums.ImportType;

public record ImportHistoryDetailResponse(
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
    String failureReason,
    List<ImportRowError> errors,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
