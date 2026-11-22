package org.ptit.meeting.modules.timetable.repository;

public final class ClassSectionQueries {

  // The same relationship defines list, semester and detail visibility.
  private static final String ACCESS_SCOPE = """
      section.status <> :excludedSectionStatus
      AND (
          section.lecturerId = :userId
          OR EXISTS (
              SELECT membership.id FROM ClassEnrollment membership
              WHERE membership.classSectionId = section.id
                  AND membership.userId = :userId
                  AND membership.status = :enrollmentStatus
          )
      )
      """;

  private static final String ONLINE_SCOPE = """
      AND EXISTS (
          SELECT onlineSchedule.id FROM ClassSchedule onlineSchedule
          WHERE onlineSchedule.classSectionId = section.id AND onlineSchedule.isOnline = true
      )
      """;

  public static final String FIND_ACCESSIBLE_ONLINE_CLASS = """
      SELECT section FROM ClassSection section
      WHERE section.id = :sectionId AND
      """ + ACCESS_SCOPE + ONLINE_SCOPE;

  public static final String FIND_SEMESTERS = """
      SELECT semester FROM Semester semester
      WHERE EXISTS (
          SELECT section.id FROM ClassSection section
          WHERE section.semesterId = semester.id AND
      """ + ACCESS_SCOPE + ONLINE_SCOPE + """
      )
      ORDER BY semester.startDate DESC, semester.id DESC
      """;

  public static final String FIND_ONLINE_CLASSES = """
      SELECT new org.ptit.meeting.modules.timetable.dto.response.ClassSectionResponse(
          section.id, section.classCode, section.groupNumber, section.practiceGroup,
          section.managementClass, subject.code, subject.name, subject.credit,
          semester.id, semester.name, semester.academicYear, semester.semesterNum,
          section.status,
          (SELECT COUNT(enrollment.id) FROM ClassEnrollment enrollment
              JOIN User student ON student.id = enrollment.userId
              WHERE enrollment.classSectionId = section.id
                  AND enrollment.status = :enrollmentStatus
                  AND student.accountType = org.ptit.meeting.modules.auth.constant.enums.AccountType.STUDENT),
          section.expectedStudentCount,
          COUNT(CASE WHEN session.status <> :cancelledStatus THEN session.id END),
          COUNT(CASE WHEN session.status = :endedStatus THEN session.id END),
          MIN(CASE WHEN session.status = :scheduledStatus AND session.scheduledStart >= :now
              THEN session.scheduledStart END)
      )
      FROM ClassSection section
      JOIN Subject subject ON subject.id = section.subjectId
      JOIN Semester semester ON semester.id = section.semesterId
      JOIN ClassSchedule schedule ON schedule.classSectionId = section.id AND schedule.isOnline = true
      LEFT JOIN MeetingSession session ON session.scheduleId = schedule.id
      WHERE
      """ + ACCESS_SCOPE + """
          AND (:semesterId IS NULL OR semester.id = :semesterId)
          AND (:academicYear IS NULL OR semester.academicYear = :academicYear)
      GROUP BY section.id, section.classCode, section.groupNumber, section.practiceGroup,
          section.managementClass, subject.code, subject.name, subject.credit,
          semester.id, semester.name, semester.academicYear, semester.semesterNum,
          semester.startDate, section.status, section.expectedStudentCount
      ORDER BY semester.startDate DESC, subject.name ASC, section.classCode ASC, section.id ASC
      """;

  // These reads are called only after ClassSectionService checks access to the class.
  private static final String CLASS_SESSIONS_SCOPE = """
      FROM MeetingSession session
      JOIN ClassSchedule schedule ON schedule.id = session.scheduleId
      WHERE schedule.classSectionId = :sectionId
          AND schedule.isOnline = true
          AND (:status IS NULL OR session.status = :status)
      """;

  public static final String FIND_CLASS_SESSIONS = """
      SELECT new org.ptit.meeting.modules.timetable.dto.response.ClassSessionResponse(
          session.id, session.publicId, session.title, session.studyDate,
          session.scheduledStart, session.scheduledEnd, session.status
      )
      """ + CLASS_SESSIONS_SCOPE + """
      ORDER BY session.scheduledStart ASC, session.id ASC
      """;

  public static final String COUNT_CLASS_SESSIONS =
      "SELECT COUNT(session.id) " + CLASS_SESSIONS_SCOPE;

  public static final String FIND_CLASS_SESSION = """
      SELECT new org.ptit.meeting.modules.timetable.dto.response.ClassSessionResponse(
          session.id, session.publicId, session.title, session.studyDate,
          session.scheduledStart, session.scheduledEnd, session.status
      )
      FROM MeetingSession session
      JOIN ClassSchedule schedule ON schedule.id = session.scheduleId
      WHERE schedule.classSectionId = :sectionId AND schedule.isOnline = true
          AND session.id = :sessionId
      """;

  private ClassSectionQueries() {
  }
}
