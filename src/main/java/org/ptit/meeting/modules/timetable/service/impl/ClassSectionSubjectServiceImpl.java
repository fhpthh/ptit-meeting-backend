package org.ptit.meeting.modules.timetable.service.impl;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ptit.meeting.modules.timetable.dto.response.ClassSectionSubjectResponse;
import org.ptit.meeting.modules.timetable.repository.ClassSectionRepository;
import org.ptit.meeting.modules.timetable.repository.SubjectRepository;
import org.ptit.meeting.modules.timetable.service.ClassSectionSubjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClassSectionSubjectServiceImpl implements ClassSectionSubjectService {
  private final ClassSectionRepository classSectionRepository;
  private final SubjectRepository subjectRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<ClassSectionSubjectResponse> findSubject(Long classSectionId) {
    if (classSectionId == null) return Optional.empty();
    return classSectionRepository.findById(classSectionId)
        .flatMap(section -> subjectRepository.findById(section.getSubjectId()))
        .map(subject -> new ClassSectionSubjectResponse(subject.getCode(), subject.getName()));
  }
}
