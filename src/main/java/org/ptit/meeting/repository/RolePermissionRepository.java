package org.ptit.meeting.repository;

import java.util.List;
import org.ptit.meeting.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

  List<RolePermission> findByRoleId(Long roleId);
}
