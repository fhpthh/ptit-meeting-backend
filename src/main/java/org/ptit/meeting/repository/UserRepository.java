package org.ptit.meeting.repository;

import java.util.Optional;
import org.ptit.meeting.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  Optional<User> findByCode(String code);

  Optional<User> findByEmailOrCode(String email, String code);

  boolean existsByEmail(String email);

  boolean existsByCode(String code);
}
