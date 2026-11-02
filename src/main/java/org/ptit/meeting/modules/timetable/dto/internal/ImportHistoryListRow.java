package org.ptit.meeting.modules.timetable.dto.internal;

import java.time.LocalDateTime;
import org.ptit.meeting.modules.timetable.constant.enums.ImportStatus;
import org.ptit.meeting.modules.timetable.constant.enums.ImportType;

public record ImportHistoryListRow(
    Long id,
    Long semesterId,
    Long uploadedBy,
    String fileName,
    ImportType importType,
    Integer totalRows,
    Integer successRows,
    Integer failedRows,
    ImportStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

}
