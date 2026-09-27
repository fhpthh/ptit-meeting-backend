
package org.ptit.meeting.modules.auth.repository;

import java.util.List;
import org.ptit.meeting.modules.auth.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

  List<RolePermission> findByRoleId(Long roleId);
}
