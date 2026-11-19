package org.ptit.meeting.modules.auth.repository;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.student.constant.enums.StudyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentAdminRepository extends JpaRepository<User, Long> {
  @Query(StudentAdminQueries.SEARCH)
  Page<User> search(@Param("type") AccountType type, @Param("status") UserStatus status,
      @Param("department") String department,
      @Param("campusId") Long campusId,
      @Param("cohortId") Long cohortId,
      @Param("majorId") Long majorId,
      @Param("administrativeClassId") Long administrativeClassId,
      @Param("studyStatus") StudyStatus studyStatus,
      @Param("pattern") String pattern, Pageable pageable);

  @Query(StudentAdminQueries.DEPARTMENTS)
  List<String> findDepartments(@Param("type") AccountType type);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(StudentAdminQueries.LOCK_ACCOUNTS)
  List<User> lockAccounts(@Param("type") AccountType type, @Param("ids") Collection<Long> ids);

  long countByAccountTypeAndStatus(AccountType type, UserStatus status);

  Optional<User> findByIdAndAccountType(Long id, AccountType type);
}
