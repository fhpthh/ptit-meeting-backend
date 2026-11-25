package org.ptit.meeting.modules.auth.service;

import org.ptit.meeting.modules.auth.dto.response.LecturerImportResponse;
import org.springframework.web.multipart.MultipartFile;

public interface LecturerImportService {
  /**
   * Đọc file Excel danh sách giảng viên, tự sinh email nếu thiếu và lưu vào hệ thống.
   */
  LecturerImportResponse importLecturersFromExcel(MultipartFile file);
}