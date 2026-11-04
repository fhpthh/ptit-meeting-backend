package org.ptit.meeting.modules.timetable.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.timetable.dto.internal.ImportHistoryListRow;
import org.ptit.meeting.modules.timetable.dto.response.ImportHistoryDetailResponse;
import org.ptit.meeting.modules.timetable.dto.response.ImportHistoryResponse;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;
import org.ptit.meeting.modules.timetable.entity.ImportHistory;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImportHistoryMapper {

  private final ObjectMapper objectMapper;

  public ImportHistoryResponse toResponse(ImportHistoryListRow row, Semester semester, User uploader) {
    String semesterCode = semester != null ? semester.getCode() : null;
    String semesterName = semester != null ? semester.getName() : null;
    boolean hasErrors = row.failedRows() != null && row.failedRows() > 0;

    return new ImportHistoryResponse(
        row.id(),
        row.semesterId(),
        semesterCode,
        semesterName,
        row.uploadedBy(),
        uploader != null ? uploader.getCode() : null,
        uploader != null ? uploader.getFullName() : null,
        row.fileName(),
        row.importType(),
        row.totalRows() != null ? row.totalRows() : 0,
        row.successRows() != null ? row.successRows() : 0,
        row.failedRows() != null ? row.failedRows() : 0,
        row.status(),
        hasErrors,
        row.createdAt(),
        row.updatedAt()
    );
  }

  public ImportHistoryResponse toResponse(ImportHistory history, Semester semester, User uploader) {
    String semesterCode = semester != null ? semester.getCode() : null;
    String semesterName = semester != null ? semester.getName() : null;
    boolean hasErrors = history.getFailedRows() != null && history.getFailedRows() > 0;

    return new ImportHistoryResponse(
        history.getId(),
        history.getSemesterId(),
        semesterCode,
        semesterName,
        history.getUploadedBy(),
        uploader != null ? uploader.getCode() : null,
        uploader != null ? uploader.getFullName() : null,
        history.getFileName(),
        history.getImportType(),
        history.getTotalRows() != null ? history.getTotalRows() : 0,
        history.getSuccessRows() != null ? history.getSuccessRows() : 0,
        history.getFailedRows() != null ? history.getFailedRows() : 0,
        history.getStatus(),
        hasErrors,
        history.getCreatedAt(),
        history.getUpdatedAt()
    );
  }

  public ImportHistoryDetailResponse toDetailResponse(
      ImportHistory history,
      Semester semester,
      User uploader
  ) {
    String semesterCode = semester != null ? semester.getCode() : null;
    String semesterName = semester != null ? semester.getName() : null;

    List<ImportRowError> errors = parseErrors(history.getErrorLog());
    String failureReason = parseFailureReason(history.getErrorLog());

    return new ImportHistoryDetailResponse(
        history.getId(),
        history.getSemesterId(),
        semesterCode,
        semesterName,
        history.getUploadedBy(),
        uploader != null ? uploader.getCode() : null,
        uploader != null ? uploader.getFullName() : null,
        history.getFileName(),
        history.getImportType(),
        history.getTotalRows() != null ? history.getTotalRows() : 0,
        history.getSuccessRows() != null ? history.getSuccessRows() : 0,
        history.getFailedRows() != null ? history.getFailedRows() : 0,
        history.getStatus(),
        failureReason,
        errors,
        history.getCreatedAt(),
        history.getUpdatedAt()
    );
  }

  public List<ImportRowError> parseErrors(String errorLogJson) {
    if (errorLogJson == null || errorLogJson.isBlank()) {
      return Collections.emptyList();
    }

    try {
      JsonNode rootNode = objectMapper.readTree(errorLogJson);
      if (rootNode.isArray()) {
        return objectMapper.readValue(
            errorLogJson,
            new TypeReference<List<ImportRowError>>() {
            }
        );
      }
    } catch (Exception e) {
      log.warn("(parseErrors) Could not deserialize error_log as row error array, length={}",
          errorLogJson.length());
    }
    return Collections.emptyList();
  }

  public String parseFailureReason(String errorLogJson) {
    if (errorLogJson == null || errorLogJson.isBlank()) {
      return null;
    }

    try {
      JsonNode rootNode = objectMapper.readTree(errorLogJson);
      if (rootNode.isObject()) {
        if (rootNode.has("reason")) {
          return rootNode.get("reason").asText();
        }
        if (rootNode.has("error")) {
          return rootNode.get("error").asText();
        }
      }
    } catch (Exception ignored) {
    }
    return null;
  }
}
