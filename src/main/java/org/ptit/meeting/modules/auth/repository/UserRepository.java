
package org.ptit.meeting.modules.auth.repository;

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
}
