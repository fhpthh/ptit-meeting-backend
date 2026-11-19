package org.ptit.meeting.modules.student.service.impl;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.student.dto.request.StudentAcademicAssignmentRequest;
import org.ptit.meeting.modules.student.dto.response.AcademicOptionResponse;
import org.ptit.meeting.modules.student.dto.response.StudentAcademicProfileResponse;
import org.ptit.meeting.modules.student.entity.AdministrativeClass;
import org.ptit.meeting.modules.student.entity.StudentAcademicProfile;
import org.ptit.meeting.modules.student.repository.AdministrativeClassRepository;
import org.ptit.meeting.modules.student.repository.StudentAcademicProfileRepository;
import org.ptit.meeting.modules.student.service.StudentAcademicProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentAcademicProfileServiceImpl implements StudentAcademicProfileService {
  private final StudentAcademicProfileRepository profiles;
  private final AdministrativeClassRepository administrativeClasses;

  @Override
  public StudentAcademicProfileResponse findByUserId(Long userId) {
    return profiles.findByUserId(userId).map(this::toResponse).orElse(null);
  }

  @Override
  public Map<Long, StudentAcademicProfileResponse> findByUserIds(Collection<Long> userIds) {
    if (userIds.isEmpty()) {
      return Map.of();
    }
    return profiles.findAllByUserIdIn(userIds).stream().collect(Collectors.toMap(
        StudentAcademicProfile::getUserId, this::toResponse, (left, right) -> left));
  }

  @Override
  @Transactional
  public StudentAcademicProfileResponse upsert(Long userId, StudentAcademicAssignmentRequest request) {
    AdministrativeClass administrativeClass = administrativeClasses
        .findWithRelationsById(request.administrativeClassId())
        .filter(AdministrativeClass::isActive)
        .orElseThrow(() -> BusinessException.notExisted(
            "error.student-structure.class.not-found", request.administrativeClassId()));
    StudentAcademicProfile profile = profiles.findByUserId(userId)
        .orElseGet(() -> StudentAcademicProfile.builder().userId(userId).build());
    profile.setAdministrativeClass(administrativeClass);
    profile.setStudyStatus(request.studyStatus());
    return toResponse(profiles.save(profile));
  }

  private StudentAcademicProfileResponse toResponse(StudentAcademicProfile profile) {
    AdministrativeClass administrativeClass = profile.getAdministrativeClass();
    return new StudentAcademicProfileResponse(profile.getId(), profile.getStudyStatus(),
        option(administrativeClass.getCampus().getId(), administrativeClass.getCampus().getCode(),
            administrativeClass.getCampus().getName()),
        option(administrativeClass.getCohort().getId(), administrativeClass.getCohort().getCode(),
            administrativeClass.getCohort().getName()),
        option(administrativeClass.getMajor().getId(), administrativeClass.getMajor().getCode(),
            administrativeClass.getMajor().getName()),
        option(administrativeClass.getId(), administrativeClass.getCode(), administrativeClass.getName()));
  }

  private AcademicOptionResponse option(Long id, String code, String name) {
    return new AcademicOptionResponse(id, code, name);
  }
}
