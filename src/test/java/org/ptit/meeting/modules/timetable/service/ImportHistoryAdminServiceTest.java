package org.ptit.meeting.modules.timetable.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.common.service.FileStorageService;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.constant.enums.ImportStatus;
import org.ptit.meeting.modules.timetable.constant.enums.ImportType;
import org.ptit.meeting.modules.timetable.dto.internal.DownloadFileResult;
import org.ptit.meeting.modules.timetable.dto.internal.ImportHistoryListRow;
import org.ptit.meeting.modules.timetable.dto.request.ImportHistorySearchRequest;
import org.ptit.meeting.modules.timetable.dto.response.ImportHistoryDetailResponse;
import org.ptit.meeting.modules.timetable.dto.response.ImportHistoryResponse;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;
import org.ptit.meeting.modules.timetable.entity.ImportHistory;
import org.ptit.meeting.modules.timetable.mapper.ImportHistoryMapper;
import org.ptit.meeting.modules.timetable.repository.ImportHistoryRepository;
import org.ptit.meeting.modules.timetable.repository.SemesterRepository;
import org.ptit.meeting.modules.timetable.service.impl.ImportHistoryAdminServiceImpl;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ImportHistoryAdminServiceTest {

  @Mock
  private ImportHistoryRepository historyRepository;

  @Mock
  private SemesterRepository semesterRepository;

  @Mock
  private FileStorageService fileStorageService;

  @Mock
  private ImportHistoryMapper historyMapper;

  @InjectMocks
  private ImportHistoryAdminServiceImpl historyAdminService;

  private ImportHistory history;
  private ImportHistoryListRow listRow;
  private ImportHistoryResponse historyResponse;
  private ImportHistoryDetailResponse detailResponse;

  @BeforeEach
  void setUp() {
    LocalDateTime now = LocalDateTime.now();

    history = ImportHistory.builder()
        .id(10L)
        .semesterId(1L)
        .uploadedBy(5L)
        .fileName("1606_TKB-hoc-ky-1-nam-hoc-2026-2027.xlsx")
        .fileUrl("timetables/test.xlsx")
        .importType(ImportType.MASTER_TIMETABLE)
        .totalRows(4471)
        .successRows(4469)
        .failedRows(2)
        .status(ImportStatus.PARTIAL_SUCCESS)
        .errorLog("[{\"rowNumber\":1557,\"errorMessage\":\"Không có tuần học nào được đánh dấu x\"}]")
        .createdAt(now)
        .updatedAt(now)
        .build();

    listRow = new ImportHistoryListRow(
        10L,
        1L,
        5L,
        "1606_TKB-hoc-ky-1-nam-hoc-2026-2027.xlsx",
        ImportType.MASTER_TIMETABLE,
        4471,
        4469,
        2,
        ImportStatus.PARTIAL_SUCCESS,
        now,
        now
    );

    historyResponse = new ImportHistoryResponse(
        10L,
        1L,
        "2026-2027-HK1",
        "Học kỳ 1 năm học 2026 - 2027",
        5L,
        "1606_TKB-hoc-ky-1-nam-hoc-2026-2027.xlsx",
        ImportType.MASTER_TIMETABLE,
        4471,
        4469,
        2,
        ImportStatus.PARTIAL_SUCCESS,
        true,
        now,
        now
    );

    detailResponse = new ImportHistoryDetailResponse(
        10L,
        1L,
        "2026-2027-HK1",
        "Học kỳ 1 năm học 2026 - 2027",
        5L,
        "1606_TKB-hoc-ky-1-nam-hoc-2026-2027.xlsx",
        ImportType.MASTER_TIMETABLE,
        4471,
        4469,
        2,
        ImportStatus.PARTIAL_SUCCESS,
        null,
        List.of(new ImportRowError(1557, "Không có tuần học nào được đánh dấu x")),
        now,
        now
    );
  }

  @Test
  @DisplayName("Search import histories returns paged response without error_log")
  void search_Success() {
    Page<ImportHistoryListRow> page = new PageImpl<>(List.of(listRow));
    when(historyRepository.search(any(), any(), any(Pageable.class))).thenReturn(page);
    when(semesterRepository.findAllById(any())).thenReturn(List.of());
    when(historyMapper.toResponse(any(ImportHistoryListRow.class), any())).thenReturn(historyResponse);

    ImportHistorySearchRequest request = new ImportHistorySearchRequest();
    request.setSemesterId(1L);
    request.setStatus(ImportStatus.PARTIAL_SUCCESS);

    PageResponseDTO<ImportHistoryResponse> result = historyAdminService.search(request);

    assertNotNull(result);
    assertEquals(1, result.content().size());
    assertEquals("1606_TKB-hoc-ky-1-nam-hoc-2026-2027.xlsx", result.content().get(0).fileName());
  }

  @Test
  @DisplayName("Get errors successfully parses list of row errors")
  void getErrors_Success() {
    when(historyRepository.findById(10L)).thenReturn(Optional.of(history));
    when(historyMapper.parseErrors(history.getErrorLog())).thenReturn(List.of(
        new ImportRowError(1557, "Không có tuần học nào được đánh dấu x")
    ));

    List<ImportRowError> errors = historyAdminService.getErrors(10L);

    assertNotNull(errors);
    assertEquals(1, errors.size());
    assertEquals(1557, errors.get(0).getRowNumber());
  }

  @Test
  @DisplayName("Get errors returns empty list when errorLog is empty or null")
  void getErrors_EmptyLog_ReturnsEmptyList() {
    history.setErrorLog(null);
    when(historyRepository.findById(10L)).thenReturn(Optional.of(history));
    when(historyMapper.parseErrors(null)).thenReturn(List.of());

    List<ImportRowError> errors = historyAdminService.getErrors(10L);

    assertNotNull(errors);
    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("Get detail returns full detail response")
  void getDetail_Success() {
    when(historyRepository.findById(10L)).thenReturn(Optional.of(history));
    when(semesterRepository.findById(1L)).thenReturn(Optional.empty());
    when(historyMapper.toDetailResponse(history, null)).thenReturn(detailResponse);

    ImportHistoryDetailResponse result = historyAdminService.getDetail(10L);

    assertNotNull(result);
    assertEquals(10L, result.id());
    assertEquals(1, result.errors().size());
  }

  @Test
  @DisplayName("Get detail throws 404 when importId not found")
  void getDetail_NotFound_ThrowsException() {
    when(historyRepository.findById(999L)).thenReturn(Optional.empty());

    BusinessException ex = assertThrows(
        BusinessException.class,
        () -> historyAdminService.getDetail(999L)
    );
    assertEquals(TimetableErrorConstants.IMPORT_HISTORY_NOT_FOUND, ex.getStrCode());
  }

  @Test
  @DisplayName("Download file successfully returns resource and original fileName")
  void downloadFile_Success() {
    Resource mockResource = mock(Resource.class);
    when(mockResource.exists()).thenReturn(true);
    when(mockResource.isReadable()).thenReturn(true);

    when(historyRepository.findById(10L)).thenReturn(Optional.of(history));
    when(fileStorageService.loadFileAsResource(history.getFileUrl())).thenReturn(mockResource);

    DownloadFileResult result = historyAdminService.downloadFile(10L);

    assertNotNull(result);
    assertEquals(history.getFileName(), result.fileName());
    assertEquals(mockResource, result.resource());
  }

  @Test
  @DisplayName("Download file throws 404 when fileUrl is empty")
  void downloadFile_EmptyFileUrl_ThrowsException() {
    history.setFileUrl("");
    when(historyRepository.findById(10L)).thenReturn(Optional.of(history));

    BusinessException ex = assertThrows(
        BusinessException.class,
        () -> historyAdminService.downloadFile(10L)
    );
    assertEquals(TimetableErrorConstants.IMPORT_FILE_NOT_FOUND, ex.getStrCode());
  }

  @Test
  @DisplayName("Download file throws 404 when file does not exist on disk")
  void downloadFile_PhysicalFileNotFound_ThrowsException() {
    Resource mockResource = mock(Resource.class);
    when(mockResource.exists()).thenReturn(false);

    when(historyRepository.findById(10L)).thenReturn(Optional.of(history));
    when(fileStorageService.loadFileAsResource(history.getFileUrl())).thenReturn(mockResource);

    BusinessException ex = assertThrows(
        BusinessException.class,
        () -> historyAdminService.downloadFile(10L)
    );
    assertEquals(TimetableErrorConstants.IMPORT_FILE_NOT_FOUND, ex.getStrCode());
  }
}
