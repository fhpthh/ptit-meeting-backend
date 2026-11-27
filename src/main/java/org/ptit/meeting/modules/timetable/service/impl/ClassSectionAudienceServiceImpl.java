package org.ptit.meeting.modules.timetable.service.impl;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.exception.BusinessException;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.auth.service.UserService;
import org.ptit.meeting.modules.timetable.constant.TimetableErrorConstants;
import org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.ptit.meeting.modules.timetable.repository.ClassEnrollmentRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSectionRepository;
import org.ptit.meeting.modules.timetable.service.ClassSectionAudienceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClassSectionAudienceServiceImpl implements ClassSectionAudienceService {

  private final ClassSectionRepository classSectionRepository;
  private final ClassEnrollmentRepository classEnrollmentRepository;
  private final UserService userService;

  @Override
  @Transactional(readOnly = true)
  public Set<Long> findActiveMemberUserIds(Collection<Long> classSectionIds) {
    List<ClassSection> sections = loadAndValidateActiveSections(classSectionIds);
    Set<Long> memberIds = new LinkedHashSet<>(
        classEnrollmentRepository.findActiveEnrolledStudentUserIdsByClassSectionIds(
            extractIds(sections),
            EnrollmentStatus.ENROLLED,
            AccountType.STUDENT,
            UserStatus.ACTIVE
        )
    );
    List<Long> lecturerIds = sections.stream()
        .map(ClassSection::getLecturerId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    if (!lecturerIds.isEmpty()) {
      memberIds.addAll(userService.findActiveUserIdsByIds(lecturerIds));
    }
    return memberIds;
  }

  @Override
  @Transactional(readOnly = true)
  public Set<Long> findOwnedClassSectionStudentIds(Long lecturerId,
      Collection<Long> classSectionIds) {
    List<ClassSection> sections = loadAndValidateActiveSections(classSectionIds);
    for (ClassSection section : sections) {
      if (section.getLecturerId() == null || !section.getLecturerId().equals(lecturerId)) {
        throw BusinessException.unauthorized(
            TimetableErrorConstants.LECTURER_SEMESTER_ACCESS_DENIED,
            section.getId()
        );
      }
    }
    return new LinkedHashSet<>(
        classEnrollmentRepository.findActiveEnrolledStudentUserIdsByClassSectionIds(
            extractIds(sections),
            EnrollmentStatus.ENROLLED,
            AccountType.STUDENT,
            UserStatus.ACTIVE
        )
    );
  }

  private List<ClassSection> loadAndValidateActiveSections(Collection<Long> classSectionIds) {
    if (classSectionIds == null || classSectionIds.isEmpty()) {
      return List.of();
    }
    Set<Long> requestedIds = classSectionIds.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toCollection(LinkedHashSet::new));
    if (requestedIds.isEmpty()) {
      return List.of();
    }

    Map<Long, ClassSection> sectionMap = classSectionRepository.findAllById(requestedIds).stream()
        .collect(Collectors.toMap(ClassSection::getId, Function.identity()));

    for (Long id : requestedIds) {
      ClassSection section = sectionMap.get(id);
      if (section == null) {
        throw BusinessException.notExisted(TimetableErrorConstants.CLASS_SECTION_NOT_FOUND, id);
      }
      if (section.getStatus() != ClassSectionStatus.ACTIVE) {
        throw BusinessException.invalidInput(TimetableErrorConstants.CLASS_SECTION_INACTIVE, id);
      }
    }
    return requestedIds.stream().map(sectionMap::get).toList();
  }

  private Set<Long> extractIds(List<ClassSection> sections) {
    return sections.stream().map(ClassSection::getId).collect(Collectors.toSet());
  }
}
