package org.ptit.meeting.modules.auth.repository;

import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LecturerAdminRepository extends JpaRepository<User, Long> {

  @Query("""
      SELECT u FROM User u WHERE u.accountType = :type
        AND (:status IS NULL OR u.status = :status)
        AND (:department IS NULL OR LOWER(u.department) = LOWER(:department))
        AND (:keyword IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(u.code) LIKE LOWER(CONCAT('%', :keyword, '%')))
      """)
  Page<User> search(
      @Param("type") AccountType type,
      @Param("status") UserStatus status,
      @Param("department") String department,
      @Param("keyword") String keyword,
      Pageable pageable
  );

  Optional<User> findByIdAndAccountType(Long id, AccountType type);
}
