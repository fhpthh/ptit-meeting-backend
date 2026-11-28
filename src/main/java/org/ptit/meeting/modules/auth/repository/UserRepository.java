
package org.ptit.meeting.modules.auth.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  Optional<User> findByEmailIgnoreCase(String email);

  List<User> findAllByFullNameIgnoreCaseAndAccountTypeAndStatus(
      String fullName, AccountType accountType, UserStatus status);

  @Query("""
      SELECT u FROM User u WHERE u.accountType = :type AND u.status = :status
        AND (:keyword IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
          OR LOWER(u.code) LIKE LOWER(CONCAT('%', :keyword, '%')))
      ORDER BY u.fullName, u.id
      """)
  List<User> searchActiveLecturers(@Param("type") AccountType type,
      @Param("status") UserStatus status, @Param("keyword") String keyword, Pageable pageable);

  Optional<User> findByCode(String code);

  Optional<User> findByEmailOrCode(String email, String code);

  boolean existsByEmail(String email);

  boolean existsByCode(String code);

  List<User> findAllByCodeIgnoreCase(String code);

  List<User> findAllByFullNameIgnoreCase(String fullName);

  List<User> findAllByAccountTypeAndDepartmentIsNotNull(AccountType accountType);

  long countByAccountType(AccountType accountType);

  long countByAccountTypeAndStatus(AccountType accountType, UserStatus status);

  @Query("""
      SELECT u.id FROM User u
      WHERE u.status = :status
        AND u.accountType IN :accountTypes
      ORDER BY u.id ASC
      """)
  List<Long> findIdsByAccountTypeInAndStatus(
      @Param("accountTypes") Collection<AccountType> accountTypes,
      @Param("status") UserStatus status
  );

  @Query("""
      SELECT u.id FROM User u
      WHERE u.status = :status
        AND u.id IN :ids
      ORDER BY u.id ASC
      """)
  List<Long> findIdsByIdInAndStatus(
      @Param("ids") Collection<Long> ids,
      @Param("status") UserStatus status
  );

  @Query("""
      SELECT u FROM User u
      WHERE u.status = :status
        AND LOWER(u.email) IN :emails
      ORDER BY u.id ASC
      """)
  List<User> findByEmailLowerInAndStatus(
      @Param("emails") Collection<String> emails,
      @Param("status") UserStatus status
  );

  @Query("""
      SELECT u.id FROM User u
      WHERE u.status = :status
        AND u.department IS NOT NULL
        AND (LOWER(TRIM(u.department)) = LOWER(:department)
          OR LOWER(u.department) LIKE LOWER(CONCAT('%', :department, '%')))
      ORDER BY u.id ASC
      """)
  List<Long> findIdsByDepartmentIgnoreCaseAndStatus(
      @Param("department") String department,
      @Param("status") UserStatus status
  );

  @Query("""
      SELECT DISTINCT TRIM(u.department) FROM User u
      WHERE u.status = :status
        AND u.department IS NOT NULL
        AND TRIM(u.department) <> ''
      ORDER BY TRIM(u.department) ASC
      """)
  List<String> findDistinctDepartmentsByStatus(@Param("status") UserStatus status);
}
