package org.ptit.meeting.modules.timetable.repository;

final class StudentClassEnrollmentQueries {
  static final String JOINS = """
      FROM ClassEnrollment e
      JOIN ClassSection c ON c.id = e.classSectionId
      LEFT JOIN Semester sem ON sem.id = c.semesterId
      LEFT JOIN Subject sub ON sub.id = c.subjectId
      WHERE e.userId = :userId AND (:semesterId IS NULL OR c.semesterId = :semesterId)
      """;

  static final String LIST = """
      SELECT new org.ptit.meeting.modules.timetable.dto.response.StudentClassEnrollmentResponse(
        e.id, c.id, c.classCode, c.semesterId, sem.code, sem.name, sub.code, sub.name,
        c.groupNumber, c.practiceGroup, e.status, e.enrolledAt)
      """ + JOINS + " ORDER BY e.enrolledAt DESC, e.id DESC";

  private StudentClassEnrollmentQueries() {}
}
