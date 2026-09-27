
package org.ptit.meeting.modules.auth.repository;

import java.util.Optional;
import org.ptit.meeting.modules.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

  Optional<Permission> findByCode(String code);
}
