package org.ptit.meeting.modules.timetable.event;

/**
 * Domain event published when a lecturer is assigned to an online class section.
 */
public record LecturerAssignedEvent(
    Long classSectionId,
    String classCode,
    String subjectName,
    Long lecturerId
) {
}
