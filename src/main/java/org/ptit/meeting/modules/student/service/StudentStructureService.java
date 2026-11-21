package org.ptit.meeting.modules.student.service;

import org.ptit.meeting.modules.student.dto.request.AcademicCatalogCreateRequest;
import org.ptit.meeting.modules.student.dto.request.AdministrativeClassCreateRequest;
import org.ptit.meeting.modules.student.dto.response.AcademicOptionResponse;
import org.ptit.meeting.modules.student.dto.response.AdministrativeClassOptionResponse;
import org.ptit.meeting.modules.student.dto.response.StudentStructureOptionsResponse;
import org.ptit.meeting.modules.student.dto.response.StudentStructureTreeResponse;

public interface StudentStructureService {
  StudentStructureOptionsResponse options(Long campusId, Long cohortId, Long majorId);
  StudentStructureTreeResponse tree(String keyword);
  AcademicOptionResponse createCampus(AcademicCatalogCreateRequest request);
  AcademicOptionResponse createCohort(AcademicCatalogCreateRequest request);
  AcademicOptionResponse createMajor(AcademicCatalogCreateRequest request);
  AdministrativeClassOptionResponse createAdministrativeClass(
      AdministrativeClassCreateRequest request);
}
