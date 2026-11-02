package org.ptit.meeting.modules.timetable.controller;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.timetable.dto.internal.DownloadFileResult;
import org.ptit.meeting.modules.timetable.dto.request.ImportHistorySearchRequest;
import org.ptit.meeting.modules.timetable.dto.response.ImportHistoryDetailResponse;
import org.ptit.meeting.modules.timetable.dto.response.ImportHistoryResponse;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;
import org.ptit.meeting.modules.timetable.service.ImportHistoryAdminService;
import org.ptit.meeting.utils.MessageUtil;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/timetable/import-histories")
@RequiredArgsConstructor
public class ImportHistoryAdminController {

  private static final String LIST_SUCCESS = "timetable.admin.import-history.list.success";
  private static final String DETAIL_SUCCESS = "timetable.admin.import-history.detail.success";
  private static final String ERRORS_SUCCESS = "timetable.admin.import-history.errors.success";

  private final ImportHistoryAdminService historyAdminService;
  private final MessageUtil messageUtil;

  @GetMapping
  public BaseResponse<PageResponseDTO<ImportHistoryResponse>> search(
      @Valid @ModelAttribute ImportHistorySearchRequest request
  ) {
    PageResponseDTO<ImportHistoryResponse> response = historyAdminService.search(request);
    return BaseResponse.success(messageUtil.getMessage(LIST_SUCCESS), response);
  }

  @GetMapping("/{id}")
  public BaseResponse<ImportHistoryDetailResponse> getDetail(@PathVariable Long id) {
    ImportHistoryDetailResponse response = historyAdminService.getDetail(id);
    return BaseResponse.success(messageUtil.getMessage(DETAIL_SUCCESS), response);
  }

  @GetMapping("/{id}/errors")
  public BaseResponse<List<ImportRowError>> getErrors(@PathVariable Long id) {
    List<ImportRowError> response = historyAdminService.getErrors(id);
    return BaseResponse.success(messageUtil.getMessage(ERRORS_SUCCESS), response);
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<Resource> download(@PathVariable Long id) {
    DownloadFileResult result = historyAdminService.downloadFile(id);

    ContentDisposition contentDisposition = ContentDisposition.attachment()
        .filename(result.fileName(), StandardCharsets.UTF_8)
        .build();

    ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

    if (result.contentLength() > 0) {
      builder.contentLength(result.contentLength());
    }

    return builder.body(result.resource());
  }
}