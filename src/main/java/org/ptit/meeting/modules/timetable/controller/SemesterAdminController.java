package org.ptit.meeting.modules.timetable.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.timetable.dto.request.SemesterCreateRequest;
import org.ptit.meeting.modules.timetable.dto.request.SemesterSearchRequest;
import org.ptit.meeting.modules.timetable.dto.request.SemesterUpdateRequest;
import org.ptit.meeting.modules.timetable.dto.response.SemesterResponse;
import org.ptit.meeting.modules.timetable.service.SemesterAdminService;
import org.ptit.meeting.utils.MessageUtil;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/semesters")
@RequiredArgsConstructor
public class SemesterAdminController {

  private static final String DELETE_SUCCESS_MESSAGE = "timetable.semester.delete.success";

  private final SemesterAdminService semesterAdminService;
  private final MessageUtil messageUtil;

  @PostMapping
  public BaseResponse<SemesterResponse> create(
      @Valid @RequestBody SemesterCreateRequest request
  ) {
    return BaseResponse.created(semesterAdminService.create(request));
  }

  @PutMapping("/{id}")
  public BaseResponse<SemesterResponse> update(
      @PathVariable Long id,
      @Valid @RequestBody SemesterUpdateRequest request
  ) {
    return BaseResponse.success(semesterAdminService.update(id, request));
  }

  @PatchMapping("/{id}/active")
  public BaseResponse<SemesterResponse> activate(@PathVariable Long id) {
    return BaseResponse.success(semesterAdminService.activate(id));
  }

  @GetMapping("/{id}")
  public BaseResponse<SemesterResponse> getById(@PathVariable Long id) {
    return BaseResponse.success(semesterAdminService.getById(id));
  }

  @GetMapping("/active")
  public BaseResponse<SemesterResponse> getActive() {
    return BaseResponse.success(semesterAdminService.getActive());
  }

  @DeleteMapping("/{id}")
  public BaseResponse<Void> delete(@PathVariable Long id) {
    semesterAdminService.delete(id);
    return BaseResponse.success(messageUtil.getMessage(DELETE_SUCCESS_MESSAGE));
  }

  @GetMapping
  public BaseResponse<PageResponseDTO<SemesterResponse>> search(
      SemesterSearchRequest request
  ) {
    return BaseResponse.success(semesterAdminService.search(request));
  }
}
