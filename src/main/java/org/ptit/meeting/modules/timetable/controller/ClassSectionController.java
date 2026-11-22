package org.ptit.meeting.modules.timetable.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.auth.security.CustomUserDetails;
import org.ptit.meeting.modules.meeting.constant.enums.SessionStatus;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionDetailResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionSemesterResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionStudentResponse;
import org.ptit.meeting.modules.timetable.dto.response.ClassSessionResponse;
import org.ptit.meeting.modules.timetable.service.ClassSectionService;
import org.ptit.meeting.utils.MessageUtil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/class-sections")
@RequiredArgsConstructor
public class ClassSectionController {

  private final ClassSectionService classSectionService;
  private final MessageUtil messageUtil;

  @GetMapping("/{sectionId}")
  public BaseResponse<ClassSectionDetailResponse> getDetail(
      @PathVariable Long sectionId,
      @AuthenticationPrincipal CustomUserDetails currentUser
  ) {
    return BaseResponse.success(
        messageUtil.getMessage("timetable.class-section.detail.success"),
        classSectionService.getDetail(currentUser.getUserId(), sectionId)
    );
  }

  @GetMapping("/{sectionId}/sessions")
  public BaseResponse<PageResponseDTO<ClassSessionResponse>> getSessions(
      @PathVariable Long sectionId,
      @RequestParam(required = false) SessionStatus status,
      @RequestParam(defaultValue = "0")
      @Min(value = 0, message = "{validation.page.number.min}") int page,
      @RequestParam(defaultValue = "20")
      @Min(value = 1, message = "{validation.page.size.min}")
      @Max(value = 100, message = "{validation.page.size.max}") int size,
      @AuthenticationPrincipal CustomUserDetails currentUser
  ) {
    return BaseResponse.success(
        messageUtil.getMessage("timetable.class-section.sessions.success"),
        classSectionService.getSessions(currentUser.getUserId(), sectionId, status, page, size)
    );
  }

  @GetMapping("/{sectionId}/sessions/{sessionId}")
  public BaseResponse<ClassSessionResponse> getSession(
      @PathVariable Long sectionId,
      @PathVariable Long sessionId,
      @AuthenticationPrincipal CustomUserDetails currentUser
  ) {
    return BaseResponse.success(
        classSectionService.getSession(currentUser.getUserId(), sectionId, sessionId)
    );
  }

  @GetMapping("/{sectionId}/students")
  public BaseResponse<PageResponseDTO<ClassSectionStudentResponse>> getStudents(
      @PathVariable Long sectionId,
      @RequestParam(required = false)
      @Size(max = 100, message = "{validation.timetable.keyword.size}") String keyword,
      @RequestParam(defaultValue = "studentCode") String sortBy,
      @RequestParam(defaultValue = "0")
      @Min(value = 0, message = "{validation.page.number.min}") int page,
      @RequestParam(defaultValue = "20")
      @Min(value = 1, message = "{validation.page.size.min}")
      @Max(value = 100, message = "{validation.page.size.max}") int size,
      @AuthenticationPrincipal CustomUserDetails currentUser
  ) {
    return BaseResponse.success(
        messageUtil.getMessage("timetable.class-section.students.success"),
        classSectionService.getStudents(currentUser.getUserId(), sectionId, keyword, sortBy, page, size)
    );
  }

  @GetMapping("/semesters")
  public BaseResponse<List<ClassSectionSemesterResponse>> getSemesters(
      @AuthenticationPrincipal CustomUserDetails currentUser
  ) {
    return BaseResponse.success(
        messageUtil.getMessage("timetable.class-section.semesters.success"),
        classSectionService.getSemesters(currentUser.getUserId())
    );
  }

  @GetMapping
  public BaseResponse<List<ClassSectionResponse>> getClasses(
      @RequestParam(required = false)
      @Positive(message = "{validation.timetable.semester.id.invalid}") Long semesterId,
      @RequestParam(required = false)
      @Size(max = 20, message = "{validation.timetable.semester.academic-year.size}") String academicYear,
      @AuthenticationPrincipal CustomUserDetails currentUser
  ) {
    return BaseResponse.success(
        messageUtil.getMessage("timetable.class-section.list.success"),
        classSectionService.getOnlineClasses(currentUser.getUserId(), semesterId, academicYear)
    );
  }
}
