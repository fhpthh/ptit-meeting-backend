package org.ptit.meeting.modules.auth.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.auth.constant.enums.StudentAdminExportFormat;
import org.ptit.meeting.modules.auth.dto.internal.StudentAdminExportFile;
import org.ptit.meeting.modules.auth.dto.request.StudentAdminBulkStatusRequest;
import org.ptit.meeting.modules.auth.dto.request.StudentAdminCreateRequest;
import org.ptit.meeting.modules.auth.dto.request.StudentAdminEnrollmentSearchRequest;
import org.ptit.meeting.modules.auth.dto.request.StudentAdminPasswordResetRequest;
import org.ptit.meeting.modules.auth.dto.request.StudentAdminSearchRequest;
import org.ptit.meeting.modules.auth.dto.request.StudentAdminStatusRequest;
import org.ptit.meeting.modules.auth.dto.request.StudentAdminUpdateRequest;
import org.ptit.meeting.modules.auth.dto.response.StudentAdminBulkStatusResponse;
import org.ptit.meeting.modules.auth.dto.response.StudentAdminFiltersResponse;
import org.ptit.meeting.modules.auth.dto.response.StudentAdminResponse;
import org.ptit.meeting.modules.auth.dto.response.StudentAdminSummaryResponse;
import org.ptit.meeting.modules.auth.service.StudentAdminService;
import org.ptit.meeting.modules.timetable.dto.response.StudentClassEnrollmentResponse;
import org.ptit.meeting.modules.timetable.dto.response.StudentSemesterResponse;
import org.ptit.meeting.modules.timetable.dto.response.StudentOnlineSubjectResponse;
import org.ptit.meeting.modules.timetable.dto.response.StudentSubjectOnlineScheduleResponse;
import org.ptit.meeting.modules.timetable.dto.response.StudentWeeklyTimetableResponse;
import org.ptit.meeting.modules.timetable.service.StudentTimetableService;
import org.ptit.meeting.modules.student.dto.request.StudentAcademicAssignmentRequest;
import org.ptit.meeting.modules.student.dto.response.StudentAcademicProfileResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/students")
public class StudentAdminController {
  private final StudentAdminService service;
  private final StudentTimetableService timetableService;

  @GetMapping
  public BaseResponse<PageResponseDTO<StudentAdminResponse>> search(
      @Valid @ModelAttribute StudentAdminSearchRequest request) {
    return BaseResponse.success(service.search(request));
  }

  @GetMapping("/{id}")
  public BaseResponse<StudentAdminResponse> get(@PathVariable Long id) {
    return BaseResponse.success(service.get(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BaseResponse<StudentAdminResponse> create(@Valid @RequestBody StudentAdminCreateRequest request) {
    return BaseResponse.created(service.create(request));
  }

  @PutMapping("/{id}")
  public BaseResponse<StudentAdminResponse> update(
      @PathVariable Long id, @Valid @RequestBody StudentAdminUpdateRequest request) {
    return BaseResponse.success(service.update(id, request));
  }

  @PutMapping("/{id}/academic-profile")
  public BaseResponse<StudentAcademicProfileResponse> assignAcademicProfile(
      @PathVariable Long id, @Valid @RequestBody StudentAcademicAssignmentRequest request) {
    return BaseResponse.success(service.assignAcademicProfile(id, request));
  }

  @PatchMapping("/{id}/status")
  public BaseResponse<StudentAdminResponse> changeStatus(
      @PathVariable Long id, @Valid @RequestBody StudentAdminStatusRequest request) {
    return BaseResponse.success(service.changeStatus(id, request.status()));
  }

  @PatchMapping("/bulk-status")
  public BaseResponse<StudentAdminBulkStatusResponse> changeStatuses(
      @Valid @RequestBody StudentAdminBulkStatusRequest request) {
    return BaseResponse.success(service.changeStatuses(request));
  }

  @PostMapping("/{id}/reset-password")
  public BaseResponse<Void> resetPassword(
      @PathVariable Long id, @RequestBody StudentAdminPasswordResetRequest request) {
    service.resetPassword(id, request);
    return BaseResponse.success((Void) null);
  }

  @DeleteMapping("/{id}")
  public BaseResponse<Void> archive(@PathVariable Long id) {
    service.archive(id);
    return BaseResponse.success((Void) null);
  }

  @GetMapping("/summary")
  public BaseResponse<StudentAdminSummaryResponse> summary() {
    return BaseResponse.success(service.summary());
  }

  @GetMapping("/filters")
  public BaseResponse<StudentAdminFiltersResponse> filters() {
    return BaseResponse.success(service.filters());
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(@Valid @ModelAttribute StudentAdminSearchRequest request,
      @RequestParam(defaultValue = "XLSX") StudentAdminExportFormat format) {
    StudentAdminExportFile file = service.export(request, format);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(MediaType.parseMediaType(file.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(file.filename()).build().toString())
        .body(file.content());
  }

  @GetMapping("/{id}/enrollments")
  public BaseResponse<PageResponseDTO<StudentClassEnrollmentResponse>> enrollments(
      @PathVariable Long id, @Valid @ModelAttribute StudentAdminEnrollmentSearchRequest request) {
    return BaseResponse.success(service.enrollments(id, request.getSemesterId(),
        request.getPage(), request.getSize()));
  }

  @GetMapping("/{id}/online-timetable/semesters")
  public BaseResponse<List<StudentSemesterResponse>> onlineTimetableSemesters(
      @PathVariable Long id) {
    service.get(id);
    return BaseResponse.success(timetableService.getAvailableSemesters(id));
  }

  @GetMapping("/{id}/online-timetable/weekly")
  public BaseResponse<StudentWeeklyTimetableResponse> onlineTimetable(
      @PathVariable Long id,
      @RequestParam Long semesterId,
      @RequestParam int weekNumber) {
    service.get(id);
    return BaseResponse.success(
        timetableService.getWeeklyTimetable(id, semesterId, weekNumber));
  }

  @GetMapping("/{id}/online-timetable/subjects")
  public BaseResponse<List<StudentOnlineSubjectResponse>> onlineSubjects(
      @PathVariable Long id,
      @RequestParam Long semesterId) {
    service.get(id);
    return BaseResponse.success(timetableService.getOnlineSubjects(id, semesterId));
  }

  @GetMapping("/{id}/online-timetable/subject-schedule")
  public BaseResponse<StudentSubjectOnlineScheduleResponse> subjectOnlineSchedule(
      @PathVariable Long id,
      @RequestParam Long semesterId,
      @RequestParam Long subjectId) {
    service.get(id);
    return BaseResponse.success(
        timetableService.getSubjectOnlineSchedule(id, semesterId, subjectId));
  }
}
