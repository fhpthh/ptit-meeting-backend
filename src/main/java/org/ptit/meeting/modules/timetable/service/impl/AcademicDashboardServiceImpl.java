package org.ptit.meeting.modules.timetable.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.auth.repository.UserRepository;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.timetable.dto.internal.LecturerOnlineScheduleRow;
import org.ptit.meeting.modules.timetable.dto.response.AcademicDashboardKpiResponse;
import org.ptit.meeting.modules.timetable.dto.response.AcademicDepartmentStatResponse;
import org.ptit.meeting.modules.timetable.dto.response.AcademicTodaySessionResponse;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.ptit.meeting.modules.timetable.repository.ClassSectionRepository;
import org.ptit.meeting.modules.timetable.repository.MeetingSessionRepository;
import org.ptit.meeting.modules.timetable.repository.RoomRepository;
import org.ptit.meeting.modules.timetable.repository.SemesterRepository;
import org.ptit.meeting.modules.timetable.repository.SubjectRepository;
import org.ptit.meeting.modules.timetable.service.AcademicDashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicDashboardServiceImpl implements AcademicDashboardService {

  private final SemesterRepository semesterRepository;
  private final UserRepository userRepository;
  private final ClassSectionRepository classSectionRepository;
  private final SubjectRepository subjectRepository;
  private final RoomRepository roomRepository;
  private final MeetingSessionRepository meetingSessionRepository;

  @Override
  @Transactional(readOnly = true)
  public AcademicDashboardKpiResponse getKpi() {
    Semester semester = getActiveOrLatestSemester();

    Long semesterId = semester != null ? semester.getId() : null;
    String semesterName = semester != null ? semester.getName() : "Chưa có học kỳ";
    String academicYear = semester != null ? semester.getAcademicYear() : "";
    LocalDate startDate = semester != null ? semester.getStartDate() : LocalDate.now();
    LocalDate endDate = semester != null ? semester.getEndDate() : LocalDate.now().plusMonths(4);

    int weekTotal = 19;
    if (startDate != null && endDate != null && !endDate.isBefore(startDate)) {
      weekTotal = (int) ChronoUnit.WEEKS.between(startDate, endDate) + 1;
    }

    int weekCurrent = 1;
    LocalDate today = LocalDate.now();
    if (startDate != null && !today.isBefore(startDate)) {
      long weeksBetween = ChronoUnit.WEEKS.between(startDate, today);
      weekCurrent = (int) weeksBetween + 1;
      if (weekCurrent > weekTotal) {
        weekCurrent = weekTotal;
      }
    }

    long lecturerTotal = userRepository.countByAccountType(AccountType.LECTURER);
    long lecturerActive = userRepository.countByAccountTypeAndStatus(AccountType.LECTURER, UserStatus.ACTIVE);
    long lecturerInactive = userRepository.countByAccountTypeAndStatus(AccountType.LECTURER, UserStatus.INACTIVE);

    long classSectionTotal = semesterId != null ? classSectionRepository.countBySemesterId(semesterId) : 0;
    long subjectTotal = semesterId != null ? subjectRepository.countDistinctSubjectsBySemesterId(semesterId) : 0;
    long onlineRoomTotal = roomRepository.countByStatus(RoomStatus.ACTIVE);
    long meetingSessionTotal = semesterId != null ? meetingSessionRepository.countBySemesterId(semesterId) : 0;

    return new AcademicDashboardKpiResponse(
        semesterId,
        semesterName,
        academicYear,
        weekCurrent,
        weekTotal,
        startDate,
        endDate,
        lecturerTotal,
        lecturerActive,
        lecturerInactive,
        classSectionTotal,
        subjectTotal,
        onlineRoomTotal,
        meetingSessionTotal
    );
  }

  @Override
  @Transactional(readOnly = true)
  public List<AcademicTodaySessionResponse> getTodaySessions() {
    LocalDate today = LocalDate.now();
    List<LecturerOnlineScheduleRow> rows = meetingSessionRepository.findTodayOnlineSessions(today);

    LocalDateTime now = LocalDateTime.now();
    List<AcademicTodaySessionResponse> results = new ArrayList<>();

    for (LecturerOnlineScheduleRow row : rows) {
      String shift = buildShiftString(row);
      String status = determineSessionStatus(now, row.scheduledStart(), row.scheduledEnd());
      String pin = row.runtimeSessionId() != null && row.runtimeSessionId().length() >= 4
          ? row.runtimeSessionId().substring(0, 4)
          : "8888";

      results.add(new AcademicTodaySessionResponse(
          row.sessionId(),
          shift,
          row.subjectName() != null ? row.subjectName() : "Môn học trực tuyến",
          row.subjectCode() != null ? row.subjectCode() : "",
          row.classCode() != null ? row.classCode() : "",
          row.lecturerName() != null ? row.lecturerName() : "Chưa phân công",
          row.lecturerId() != null ? "GV" + String.format(Locale.ROOT, "%03d", row.lecturerId()) : "-",
          row.roomName() != null ? row.roomName() : "Phòng trực tuyến",
          pin,
          status
      ));
    }

    return results;
  }

  @Override
  @Transactional(readOnly = true)
  public List<AcademicDepartmentStatResponse> getDepartmentStats() {
    Semester semester = getActiveOrLatestSemester();
    if (semester == null) {
      return List.of();
    }

    List<Object[]> rawCounts = classSectionRepository.countClassSectionsByDepartment(semester.getId());
    long totalClasses = 0;
    for (Object[] row : rawCounts) {
      Number countNum = (Number) row[1];
      totalClasses += countNum != null ? countNum.longValue() : 0;
    }

    List<AcademicDepartmentStatResponse> results = new ArrayList<>();
    for (Object[] row : rawCounts) {
      String deptName = row[0] != null ? row[0].toString() : "Khác";
      Number countNum = (Number) row[1];
      long count = countNum != null ? countNum.longValue() : 0;

      double pct = totalClasses > 0 ? (count * 100.0 / totalClasses) : 0.0;
      String percentage = String.format(Locale.ROOT, "%.1f%%", pct);

      String code = extractDeptCode(deptName);
      String color = getDeptColor(code);

      results.add(new AcademicDepartmentStatResponse(code, deptName, count, percentage, color));
    }

    // Sắp xếp theo số lượng lớp giảm dần
    results.sort((a, b) -> Long.compare(b.classCount(), a.classCount()));
    return results;
  }

  private Semester getActiveOrLatestSemester() {
    return semesterRepository.findByIsActiveTrue()
        .or(() -> semesterRepository.findAll().stream()
            .max((s1, s2) -> {
              if (s1.getStartDate() == null || s2.getStartDate() == null) return 0;
              return s1.getStartDate().compareTo(s2.getStartDate());
            }))
        .orElse(null);
  }

  private String buildShiftString(LecturerOnlineScheduleRow row) {
    int kipNumber = 1;
    if (row.startPeriod() != null) {
      kipNumber = (row.startPeriod() + 1) / 2;
    }
    String timeRange = "";
    if (row.scheduledStart() != null && row.scheduledEnd() != null) {
      String startStr = row.scheduledStart().toLocalTime().toString();
      String endStr = row.scheduledEnd().toLocalTime().toString();
      timeRange = " (" + startStr.substring(0, Math.min(5, startStr.length()))
          + " - " + endStr.substring(0, Math.min(5, endStr.length())) + ")";
    }
    return "Kíp " + kipNumber + timeRange;
  }

  private String determineSessionStatus(LocalDateTime now, LocalDateTime start, LocalDateTime end) {
    if (start == null || end == null) {
      return "UPCOMING";
    }
    if (now.isAfter(start) && now.isBefore(end)) {
      return "LIVE";
    }
    if (now.isAfter(end)) {
      return "ENDED";
    }
    return "UPCOMING";
  }

  private String extractDeptCode(String name) {
    String upper = name.toUpperCase(Locale.ROOT);
    if (upper.contains("THÔNG TIN") || upper.contains("CNTT")) return "KCN";
    if (upper.contains("ĐIỆN TỬ") || (upper.contains("VIỄN THÔNG 1") && upper.contains("ĐTVT"))) return "KDT";
    if (upper.contains("VIỄN THÔNG")) return "KVT";
    if (upper.contains("AN TOÀN")) return "KAT";
    if (upper.contains("CƠ BẢN")) return "KCB";
    return "KHAC";
  }

  private String getDeptColor(String code) {
    return switch (code) {
      case "KCN", "CNTT" -> "#c92a2a";
      case "KDT", "KTN", "ĐTVT" -> "#2563eb";
      case "KVT", "VT" -> "#059669";
      case "KAT", "ATTT" -> "#d97706";
      default -> "#6b7280";
    };
  }
}
