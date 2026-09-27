
package org.ptit.meeting.modules.auth.repository;

import java.util.Optional;
import java.util.Set;
import org.ptit.meeting.modules.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findByCode(String code);

  @Query("SELECT r.code FROM Role r JOIN UserRole ur ON r.id = ur.roleId WHERE ur.userId = :userId")
  Set<String> findRoleCodesByUserId(@Param("userId") Long userId);

  @Query("SELECT p.code FROM Permission p JOIN RolePermission rp ON p.id = rp.permissionId JOIN UserRole ur ON rp.roleId = ur.roleId WHERE ur.userId = :userId")
  Set<String> findPermissionCodesByUserId(@Param("userId") Long userId);
}
