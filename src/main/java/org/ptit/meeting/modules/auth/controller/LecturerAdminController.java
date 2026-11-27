package org.ptit.meeting.modules.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.common.dto.response.PageResponseDTO;
import org.ptit.meeting.common.wrapper.BaseResponse;
import org.ptit.meeting.modules.auth.dto.request.LecturerAdminCreateRequest;
import org.ptit.meeting.modules.auth.dto.request.LecturerAdminPasswordResetRequest;
import org.ptit.meeting.modules.auth.dto.request.LecturerAdminSearchRequest;
import org.ptit.meeting.modules.auth.dto.request.LecturerAdminStatusRequest;
import org.ptit.meeting.modules.auth.dto.request.LecturerAdminUpdateRequest;
import org.ptit.meeting.modules.auth.dto.response.LecturerAdminResponse;
import org.ptit.meeting.modules.auth.dto.response.LecturerImportResponse;
import org.ptit.meeting.modules.auth.service.LecturerAdminService;
import org.ptit.meeting.modules.auth.service.LecturerImportService;
import org.ptit.meeting.modules.auth.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
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
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/lecturers")
@RequiredArgsConstructor
public class LecturerAdminController {

  private final LecturerImportService lecturerImportService;
  private final LecturerAdminService lecturerAdminService;
  private final MessageService messageService;

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public BaseResponse<PageResponseDTO<LecturerAdminResponse>> search(
      @Valid @ModelAttribute LecturerAdminSearchRequest request) {
    return BaseResponse.success(lecturerAdminService.search(request));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public BaseResponse<LecturerAdminResponse> get(@PathVariable Long id) {
    return BaseResponse.success(lecturerAdminService.get(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public BaseResponse<LecturerAdminResponse> create(
      @Valid @RequestBody LecturerAdminCreateRequest request) {
    return BaseResponse.created(lecturerAdminService.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public BaseResponse<LecturerAdminResponse> update(
      @PathVariable Long id, @Valid @RequestBody LecturerAdminUpdateRequest request) {
    return BaseResponse.success(lecturerAdminService.update(id, request));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public BaseResponse<LecturerAdminResponse> changeStatus(
      @PathVariable Long id, @Valid @RequestBody LecturerAdminStatusRequest request) {
    return BaseResponse.success(lecturerAdminService.changeStatus(id, request.status()));
  }

  @PostMapping("/{id}/reset-password")
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public BaseResponse<Void> resetPassword(
      @PathVariable Long id, @RequestBody(required = false) LecturerAdminPasswordResetRequest request) {
    lecturerAdminService.resetPassword(id, request);
    return BaseResponse.success((Void) null);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public BaseResponse<Void> delete(@PathVariable Long id) {
    lecturerAdminService.delete(id);
    return BaseResponse.success((Void) null);
  }

  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
  public BaseResponse<LecturerImportResponse> importLecturers(@RequestParam("file") MultipartFile file) {
    log.info("(importLecturers) filename: {}, size: {}", file.getOriginalFilename(), file.getSize());
    LecturerImportResponse response = lecturerImportService.importLecturersFromExcel(file);
    return BaseResponse.success(messageService.getMessage("lecturer.import.success"), response);
  }
}
