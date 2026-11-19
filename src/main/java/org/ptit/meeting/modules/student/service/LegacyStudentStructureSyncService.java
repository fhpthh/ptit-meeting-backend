package org.ptit.meeting.modules.student.service;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
  private static final Pattern CLASS_CODE =
      Pattern.compile("^([A-Z]\\d{2})([A-Z]+)(\\d+)$");

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
    String classCode = student.getDepartment().strip().toUpperCase(Locale.ROOT);
    Matcher matcher = CLASS_CODE.matcher(classCode);
    if (!matcher.matches()) {
      log.debug("Skip unsupported administrative class code: {}", classCode);
      return false;
    }

    String cohortCode = matcher.group(1);
    String majorCode = matcher.group(2);
    AcademicCampus campus = campusFor(classCode);
    AdmissionCohort cohort = cohorts.findByCodeIgnoreCase(cohortCode)
        .orElseGet(() -> cohorts.save(AdmissionCohort.builder()
            .code(cohortCode).name("Khóa " + cohortCode).build()));
    AcademicMajor major = majors.findByCodeIgnoreCase(majorCode)
        .orElseGet(() -> majors.save(AcademicMajor.builder()
            .code(majorCode).name(majorCode).build()));
    AdministrativeClass administrativeClass = classes.findByCodeIgnoreCase(classCode)
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

  private AcademicCampus campusFor(String classCode) {
    String code;
    String name;
    if (classCode.startsWith("D")) {
      code = "HN";
      name = "Cơ sở Hà Nội";
    } else if (classCode.startsWith("N")) {
      code = "HCM";
      name = "Cơ sở TP. Hồ Chí Minh";
    } else {
      code = "PTIT";
      name = "Học viện Công nghệ Bưu chính Viễn thông";
    }
    return campuses.findByCodeIgnoreCase(code)
        .orElseGet(() -> campuses.save(AcademicCampus.builder().code(code).name(name).build()));
  }
}
