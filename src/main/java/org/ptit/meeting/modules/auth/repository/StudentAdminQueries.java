package org.ptit.meeting.modules.auth.repository;

final class StudentAdminQueries {
  static final String SEARCH = """
      SELECT u FROM User u
      LEFT JOIN StudentAcademicProfile p ON p.userId = u.id
      LEFT JOIN p.administrativeClass ac
      WHERE u.accountType = :type
        AND (:status IS NULL OR u.status = :status)
        AND (:department IS NULL OR LOWER(TRIM(u.department)) = :department)
        AND (:campusId IS NULL OR ac.campus.id = :campusId)
        AND (:cohortId IS NULL OR ac.cohort.id = :cohortId)
        AND (:majorId IS NULL OR ac.major.id = :majorId)
        AND (:administrativeClassId IS NULL OR ac.id = :administrativeClassId)
        AND (:studyStatus IS NULL OR p.studyStatus = :studyStatus)
        AND (:pattern IS NULL OR LOWER(u.fullName) LIKE :pattern ESCAPE '!'
          OR LOWER(u.email) LIKE :pattern ESCAPE '!'
          OR LOWER(u.code) LIKE :pattern ESCAPE '!')
      """;

  static final String DEPARTMENTS = """
      SELECT DISTINCT TRIM(u.department) FROM User u
      WHERE u.accountType = :type AND TRIM(u.department) <> ''
      ORDER BY TRIM(u.department)
      """;

  static final String LOCK_ACCOUNTS = """
      SELECT u FROM User u WHERE u.accountType = :type AND u.id IN :ids ORDER BY u.id
      """;

  private StudentAdminQueries() {}
}
