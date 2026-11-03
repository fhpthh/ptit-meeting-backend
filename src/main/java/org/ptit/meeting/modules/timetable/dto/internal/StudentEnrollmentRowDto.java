package org.ptit.meeting.modules.timetable.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO dữ liệu nội bộ từng dòng trong file Excel danh sách sinh viên (DSSV).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrollmentRowDto {

  private Integer stt;
  private String studentCode;
  private String fullName;
  private String dob;
  private String managementClass;
}