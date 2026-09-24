package org.ptit.meeting.layer.application.port.outbound;

import java.util.Optional;
import java.util.Set;
import org.ptit.meeting.layer.domain.model.Role;

public interface RoleRepositoryPort {

  Optional<Role> findByCode(String code);

  Set<String> findRoleCodesByUserId(Long userId);

  Set<String> fndPermissionCodesByUserId(Long userId);

  void assignRoleToUser(Long userId, Long roleId);
}
