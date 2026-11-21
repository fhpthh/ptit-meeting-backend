package org.ptit.meeting.modules.student.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.repository.UserRepository;
import org.ptit.meeting.modules.student.constant.enums.StudyStatus;
import org.ptit.meeting.modules.student.entity.AcademicCampus;
import org.ptit.meeting.modules.student.entity.AcademicMajor;
import org.ptit.meeting.modules.student.entity.AdministrativeClass;
import org.ptit.meeting.modules.student.entity.AdmissionCohort;
import org.ptit.meeting.modules.student.entity.StudentAcademicProfile;
import org.ptit.meeting.modules.student.repository.AcademicCampusRepository;
import org.ptit.meeting.modules.student.repository.AcademicMajorRepository;
import org.ptit.meeting.modules.student.repository.AdministrativeClassRepository;
import org.ptit.meeting.modules.student.repository.AdmissionCohortRepository;
import org.ptit.meeting.modules.student.repository.StudentAcademicProfileRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Keeps the normalized academic structure compatible with legacy student imports. */
@Slf4j
@Service
@RequiredArgsConstructor
public class LegacyStudentStructureSyncService {
  private final StudentStructureResolver resolver;

  private final UserRepository users;
  private final AcademicCampusRepository campuses;
  private final AdmissionCohortRepository cohorts;
  private final AcademicMajorRepository majors;
  private final AdministrativeClassRepository classes;
  private final StudentAcademicProfileRepository profiles;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void synchronizeExistingStudents() {
    int synchronizedCount = 0;
    for (User student : users.findAllByAccountTypeAndDepartmentIsNotNull(AccountType.STUDENT)) {
      if (synchronize(student)) {
        synchronizedCount++;
      }
    }
    if (synchronizedCount > 0) {
      log.info("Synchronized academic structure for {} legacy students", synchronizedCount);
    }
  }

  @Transactional
  public boolean synchronize(User student) {
    if (student == null || student.getId() == null || student.getDepartment() == null) {
      return false;
    }
    var resolved = resolver.resolve(student.getCode(), student.getDepartment());
    if (resolved.isEmpty()) {
      log.warn("Academic structure requires review for student {}: code={}, class={}",
          student.getId(), student.getCode(), student.getDepartment());
      return false;
    }
    var structure = resolved.get();
    String classCode = structure.classCode();
    String cohortCode = structure.cohortCode();
    String majorCode = structure.majorCode();
    var existingClass = classes.findByCodeIgnoreCase(classCode);
    if (existingClass.isPresent()
        && (!existingClass.get().getCampus().getCode().equalsIgnoreCase(structure.campusCode())
            || !existingClass.get().getCohort().getCode().equalsIgnoreCase(cohortCode))) {
      log.warn("Conflicting campus/cohort for administrative class {}; student {} needs review",
          classCode, student.getId());
      return false;
    }
    AcademicCampus campus = campusFor(structure.campusCode());
    AdmissionCohort cohort = cohorts.findByCodeIgnoreCase(cohortCode)
        .orElseGet(() -> cohorts.save(AdmissionCohort.builder()
            .code(cohortCode).name("Khóa " + cohortCode).build()));
    AcademicMajor major = majors.findByCodeIgnoreCase(majorCode)
        .orElseGet(() -> majors.save(AcademicMajor.builder()
            .code(majorCode).name(majorCode).build()));
    AdministrativeClass administrativeClass = existingClass
        .orElseGet(() -> classes.save(AdministrativeClass.builder()
            .code(classCode).name(classCode).campus(campus).cohort(cohort).major(major).build()));

    StudentAcademicProfile profile = profiles.findByUserId(student.getId())
        .orElseGet(() -> StudentAcademicProfile.builder()
            .userId(student.getId()).studyStatus(StudyStatus.ENROLLED).build());
    if (profile.getAdministrativeClass() != null
        && profile.getAdministrativeClass().getId().equals(administrativeClass.getId())) {
      return false;
    }
    profile.setAdministrativeClass(administrativeClass);
    profiles.save(profile);
    return true;
  }

  private AcademicCampus campusFor(String campusCode) {
    String name = campusCode.equals("HN") ? "Cơ sở Hà Nội" : "Cơ sở TP. Hồ Chí Minh";
    return campuses.findByCodeIgnoreCase(campusCode)
        .orElseGet(() -> campuses.save(AcademicCampus.builder()
            .code(campusCode).name(name).build()));
  }
}
