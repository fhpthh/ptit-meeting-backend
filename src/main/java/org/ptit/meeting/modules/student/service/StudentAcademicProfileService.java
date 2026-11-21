package org.ptit.meeting.modules.student.service;

import java.util.Collection;
import java.util.Map;
import org.ptit.meeting.modules.student.dto.request.StudentAcademicAssignmentRequest;
import org.ptit.meeting.modules.student.dto.response.StudentAcademicProfileResponse;

public interface StudentAcademicProfileService {
  StudentAcademicProfileResponse findByUserId(Long userId);
  Map<Long, StudentAcademicProfileResponse> findByUserIds(Collection<Long> userIds);
  StudentAcademicProfileResponse upsert(Long userId, StudentAcademicAssignmentRequest request);
}
