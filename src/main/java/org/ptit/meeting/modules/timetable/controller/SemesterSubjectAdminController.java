package org.ptit.meeting.modules.timetable.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.timetable.dto.internal.ExportFileResult;
import org.ptit.meeting.modules.timetable.dto.request.SemesterSubjectSearchRequest;
import org.ptit.meeting.modules.timetable.dto.request.ClassSectionSearchRequest;
import org.ptit.meeting.modules.timetable.dto.request.ClassSectionStudentSearchRequest;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionDetailResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionStudentResponse;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectDetailResponse;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectFilterResponse;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectResponse;
import org.ptit.meeting.modules.timetable.dto.response.SemesterSubjectSectionsResponse;
import org.ptit.meeting.modules.timetable.service.SemesterSubjectAdminService;
import org.ptit.meeting.utils.MessageUtil;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/semesters/{semesterId}/subjects")
@RequiredArgsConstructor
public class SemesterSubjectAdminController {

  private static final String LIST_SUCCESS = "timetable.admin.semester-subject.list.success";
  private static final String FILTERS_SUCCESS = "timetable.admin.semester-subject.filters.success";
  private static final String DETAIL_SUCCESS = "timetable.admin.semester-subject.detail.success";
  private static final String SECTIONS_SUCCESS =
      "timetable.admin.semester-subject.sections.success";
  private static final String SECTION_DETAIL_SUCCESS =
      "timetable.admin.semester-subject.section-detail.success";
  private static final String SECTION_STUDENTS_SUCCESS =
      "timetable.admin.semester-subject.section-students.success";

  private final SemesterSubjectAdminService semesterSubjectAdminService;
  private final MessageUtil messageUtil;

  @GetMapping
  public BaseResponse<PageResponseDTO<SemesterSubjectResponse>> search(
      @PathVariable Long semesterId,
      @Valid @ModelAttribute SemesterSubjectSearchRequest request
  ) {
    return BaseResponse.success(
        messageUtil.getMessage(LIST_SUCCESS),
        semesterSubjectAdminService.search(semesterId, request)
    );
  }

  @GetMapping("/filters")
  public BaseResponse<SemesterSubjectFilterResponse> getFilters(@PathVariable Long semesterId) {
    return BaseResponse.success(
        messageUtil.getMessage(FILTERS_SUCCESS),
        semesterSubjectAdminService.getFilters(semesterId)
    );
  }

  @GetMapping("/{subjectId}")
  public BaseResponse<SemesterSubjectDetailResponse> getDetail(
      @PathVariable Long semesterId,
      @PathVariable Long subjectId,
      @RequestParam @NotBlank(message = "{validation.timetable.subject.cohort.required}") String cohort
  ) {
    return BaseResponse.success(
        messageUtil.getMessage(DETAIL_SUCCESS),
        semesterSubjectAdminService.getDetail(semesterId, subjectId, cohort)
    );
  }

  @GetMapping("/{subjectId}/sections")
  public BaseResponse<SemesterSubjectSectionsResponse> searchSections(
      @PathVariable Long semesterId,
      @PathVariable Long subjectId,
      @Valid @ModelAttribute ClassSectionSearchRequest request
  ) {
    return BaseResponse.success(
        messageUtil.getMessage(SECTIONS_SUCCESS),
        semesterSubjectAdminService.searchSections(semesterId, subjectId, request)
    );
  }

  @GetMapping("/{subjectId}/sections/{sectionId}")
  public BaseResponse<ClassSectionDetailResponse> getSectionDetail(
      @PathVariable Long semesterId,
      @PathVariable Long subjectId,
      @PathVariable Long sectionId,
      @RequestParam @NotBlank(message = "{validation.timetable.subject.cohort.required}")
      String cohort
  ) {
    return BaseResponse.success(
        messageUtil.getMessage(SECTION_DETAIL_SUCCESS),
        semesterSubjectAdminService.getSectionDetail(
            semesterId,
            subjectId,
            sectionId,
            cohort
        )
    );
  }

  @GetMapping("/{subjectId}/sections/{sectionId}/students")
  public BaseResponse<PageResponseDTO<ClassSectionStudentResponse>> searchSectionStudents(
      @PathVariable Long semesterId,
      @PathVariable Long subjectId,
      @PathVariable Long sectionId,
      @Valid @ModelAttribute ClassSectionStudentSearchRequest request
  ) {
    return BaseResponse.success(
        messageUtil.getMessage(SECTION_STUDENTS_SUCCESS),
        semesterSubjectAdminService.searchSectionStudents(
            semesterId,
            subjectId,
            sectionId,
            request
        )
    );
  }

  @GetMapping("/export")
  public ResponseEntity<byte[]> export(
      @PathVariable Long semesterId,
      @Valid @ModelAttribute SemesterSubjectSearchRequest request
  ) {
    ExportFileResult result = semesterSubjectAdminService.export(semesterId, request);
    ContentDisposition disposition = ContentDisposition.attachment()
        .filename(result.fileName(), StandardCharsets.UTF_8)
        .build();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ))
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .contentLength(result.content().length)
        .body(result.content());
  }
}
