package org.ptit.meeting.modules.timetable.service;

import java.util.Collection;
import java.util.Set;

/**
 * Read-only service exposing class section audience membership and lecturer ownership checks
 * for cross-module consumers such as notifications.
 */
public interface ClassSectionAudienceService {

  /**
   * Resolves active members (enrolled active students and assigned lecturers) of the given
   * active class sections.
   */
  Set<Long> findActiveMemberUserIds(Collection<Long> classSectionIds);

  /**
   * Verifies that all given class sections exist, are active, and are assigned to the specified
   * lecturer, then returns active enrolled student user IDs.
   */
  Set<Long> findOwnedClassSectionStudentIds(Long lecturerId, Collection<Long> classSectionIds);
}
