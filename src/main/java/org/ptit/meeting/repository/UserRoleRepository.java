package org.ptit.meeting.repository;

import java.util.List;
import org.ptit.meeting.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

  List<UserRole> findByUserId(Long userId);

  boolean existsByUserIdAndRoleId(Long userId, Long roleId);

  void deleteByUserId(Long userId);
}
