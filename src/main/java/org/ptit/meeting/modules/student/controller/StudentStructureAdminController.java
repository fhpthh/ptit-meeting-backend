package org.ptit.meeting.modules.student.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.student.dto.request.AcademicCatalogCreateRequest;
import org.ptit.meeting.modules.student.dto.request.AdministrativeClassCreateRequest;
import org.ptit.meeting.modules.student.dto.response.AcademicOptionResponse;
import org.ptit.meeting.modules.student.dto.response.AdministrativeClassOptionResponse;
import org.ptit.meeting.modules.student.dto.response.StudentStructureOptionsResponse;
import org.ptit.meeting.modules.student.dto.response.StudentStructureTreeResponse;
import org.ptit.meeting.modules.student.service.StudentStructureService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/student-structure")
public class StudentStructureAdminController {
  private final StudentStructureService service;

  @GetMapping("/options")
  public BaseResponse<StudentStructureOptionsResponse> options(
      @RequestParam(required = false) Long campusId,
      @RequestParam(required = false) Long cohortId,
      @RequestParam(required = false) Long majorId) {
    return BaseResponse.success(service.options(campusId, cohortId, majorId));
  }

  @GetMapping("/tree")
  public BaseResponse<StudentStructureTreeResponse> tree(
      @RequestParam(required = false) String keyword) {
    return BaseResponse.success(service.tree(keyword));
  }

  @PostMapping("/campuses")
  @ResponseStatus(HttpStatus.CREATED)
  public BaseResponse<AcademicOptionResponse> createCampus(
      @Valid @RequestBody AcademicCatalogCreateRequest request) {
    return BaseResponse.created(service.createCampus(request));
  }

  @PostMapping("/cohorts")
  @ResponseStatus(HttpStatus.CREATED)
  public BaseResponse<AcademicOptionResponse> createCohort(
      @Valid @RequestBody AcademicCatalogCreateRequest request) {
    return BaseResponse.created(service.createCohort(request));
  }

  @PostMapping("/majors")
  @ResponseStatus(HttpStatus.CREATED)
  public BaseResponse<AcademicOptionResponse> createMajor(
      @Valid @RequestBody AcademicCatalogCreateRequest request) {
    return BaseResponse.created(service.createMajor(request));
  }

  @PostMapping("/administrative-classes")
  @ResponseStatus(HttpStatus.CREATED)
  public BaseResponse<AdministrativeClassOptionResponse> createAdministrativeClass(
      @Valid @RequestBody AdministrativeClassCreateRequest request) {
    return BaseResponse.created(service.createAdministrativeClass(request));
  }
}
