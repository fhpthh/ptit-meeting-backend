package org.ptit.meeting.modules.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import org.ptit.meeting.modules.student.dto.request.StudentAcademicAssignmentRequest;

public record StudentAdminUpdateRequest(
    @NotBlank(message = "{validation.student-admin.required}")
    @Email(message = "{validation.student-admin.email.invalid}")
    @Size(max = 150, message = "{validation.student-admin.text.size}") String email,
    @NotBlank(message = "{validation.student-admin.required}")
    @Pattern(regexp = "[A-Za-z0-9_-]{1,30}", message = "{validation.student-admin.code.invalid}")
    String code,
    @NotBlank(message = "{validation.student-admin.required}")
    @Size(max = 150, message = "{validation.student-admin.text.size}") String fullName,
    @Size(max = 150, message = "{validation.student-admin.text.size}") String department,
    @Valid StudentAcademicAssignmentRequest academicProfile
) {
  public StudentAdminUpdateRequest(String email, String code, String fullName, String department) {
    this(email, code, fullName, department, null);
  }
}
