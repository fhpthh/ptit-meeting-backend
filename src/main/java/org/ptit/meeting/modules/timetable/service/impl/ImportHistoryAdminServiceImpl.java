package org.ptit.meeting.modules.timetable.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.common.service.FileStorageService;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.dto.internal.DownloadFileResult;
import org.ptit.meeting.modules.timetable.dto.internal.ImportHistoryListRow;
import org.ptit.meeting.modules.timetable.dto.request.ImportHistorySearchRequest;
import org.ptit.meeting.modules.timetable.dto.response.ImportHistoryDetailResponse;
import org.ptit.meeting.modules.timetable.dto.response.ImportHistoryResponse;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;
import org.ptit.meeting.modules.timetable.entity.ImportHistory;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.ptit.meeting.modules.timetable.mapper.ImportHistoryMapper;
import org.ptit.meeting.modules.timetable.repository.ImportHistoryRepository;
import org.ptit.meeting.modules.timetable.repository.SemesterRepository;
import org.ptit.meeting.modules.timetable.service.ImportHistoryAdminService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportHistoryAdminServiceImpl implements ImportHistoryAdminService {

  private final ImportHistoryRepository historyRepository;
  private final SemesterRepository semesterRepository;
  private final FileStorageService fileStorageService;
  private final ImportHistoryMapper historyMapper;

  @Override
  @Transactional(readOnly = true)
  public PageResponseDTO<ImportHistoryResponse> search(ImportHistorySearchRequest request) {
    if (request == null) {
      request = new ImportHistorySearchRequest();
    }

    Sort.Direction direction =
        request.getDirection() != null ? request.getDirection() : Sort.Direction.DESC;
    String sortBy = normalizeSortField(request.getSortBy());

    PageRequest pageable = PageRequest.of(
        Math.max(request.getPage(), 0),
        Math.min(Math.max(request.getSize(), 1), 100),
        Sort.by(direction, sortBy)
    );

    Page<ImportHistoryListRow> page = historyRepository.search(
        request.getSemesterId(),
        request.getStatus(),
        pageable
    );

    Set<Long> semesterIds = page.getContent().stream()
        .map(ImportHistoryListRow::semesterId)
        .collect(Collectors.toSet());

    Map<Long, Semester> semesterMap = semesterRepository.findAllById(semesterIds).stream()
        .collect(Collectors.toMap(Semester::getId, s -> s));

    Page<ImportHistoryResponse> responsePage = page.map(
        row -> historyMapper.toResponse(row, semesterMap.get(row.semesterId()))
    );

    return PageResponseDTO.from(responsePage);
  }

  @Override
  @Transactional(readOnly = true)
  public ImportHistoryDetailResponse getDetail(Long id) {
    ImportHistory history = getHistoryEntity(id);
    Semester semester = semesterRepository.findById(history.getSemesterId()).orElse(null);
    return historyMapper.toDetailResponse(history, semester);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ImportRowError> getErrors(Long id) {
    ImportHistory history = getHistoryEntity(id);
    return historyMapper.parseErrors(history.getErrorLog());
  }

  @Override
  @Transactional(readOnly = true)
  public DownloadFileResult downloadFile(Long id) {
    ImportHistory history = getHistoryEntity(id);

    if (history.getFileUrl() == null || history.getFileUrl().isBlank()) {
      throw BusinessException.notExisted(
          TimetableErrorConstants.IMPORT_FILE_NOT_FOUND, id
      );
    }

    Resource resource;
    try {
      resource = fileStorageService.loadFileAsResource(history.getFileUrl());
    } catch (Exception ex) {
      log.error("(downloadFile) Physical file not found on disk: {}", history.getFileUrl());
      throw BusinessException.notExisted(
          TimetableErrorConstants.IMPORT_FILE_NOT_FOUND, id
      );
    }

    if (!resource.exists() || !resource.isReadable()) {
      throw BusinessException.notExisted(
          TimetableErrorConstants.IMPORT_FILE_NOT_FOUND, id
      );
    }

    long contentLength = -1L;
    try {
      contentLength = resource.contentLength();
    } catch (IOException ignored) {
    }

    return new DownloadFileResult(
        resource,
        history.getFileName(),
        contentLength
    );
  }

  private ImportHistory getHistoryEntity(Long id) {
    return historyRepository.findById(id)
        .orElseThrow(() -> BusinessException.notExisted(
            TimetableErrorConstants.IMPORT_HISTORY_NOT_FOUND, id
        ));
  }

  private String normalizeSortField(String value) {
    if (value == null || value.isBlank()) {
      return "createdAt";
    }
    return switch (value) {
      case "createdAt", "updatedAt", "totalRows", "successRows", "failedRows", "status" -> value;
      default -> "createdAt";
    };
  }
}