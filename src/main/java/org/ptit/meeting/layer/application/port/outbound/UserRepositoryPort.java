package org.ptit.meeting.layer.application.port.outbound;

import java.util.Optional;
import org.ptit.meeting.layer.domain.model.User;

public interface UserRepositoryPort {

  Optional<User> findById(Long id);

  Optional<User> findByEmail(String email);

  Optional<User> findByEmailOrCode(String identifier);

  User save(User user);

  boolean existsByEmail(String email);
}
