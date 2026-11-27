package org.ptit.meeting.modules.timetable.event;

import java.util.Set;

/**
 * Domain event published when students are newly enrolled into an online class section.
 */
public record StudentEnrolledInClassSectionEvent(
    Long classSectionId,
    String classCode,
    Set<Long> newlyEnrolledStudentIds
) {
}
