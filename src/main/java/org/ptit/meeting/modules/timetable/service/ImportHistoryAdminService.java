package org.ptit.meeting.modules.timetable.service;

import java.util.List;
import org.ptit.meeting.common.dto.request.PageRequestDTO;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.modules.timetable.dto.internal.DownloadFileResult;
import org.ptit.meeting.modules.timetable.dto.request.ImportHistorySearchRequest;
import org.ptit.meeting.modules.timetable.dto.response.ImportHistoryDetailResponse;
import org.ptit.meeting.modules.timetable.dto.response.ImportHistoryResponse;
import org.ptit.meeting.modules.timetable.dto.response.ImportRowError;

public interface ImportHistoryAdminService {

  PageResponseDTO<ImportHistoryResponse> search(ImportHistorySearchRequest request);

  ImportHistoryDetailResponse getDetail(Long id);

  List<ImportRowError> getErrors(Long id);

  PageResponseDTO<ImportRowError> getErrors(Long id, PageRequestDTO request);

  DownloadFileResult downloadFile(Long id);
}
