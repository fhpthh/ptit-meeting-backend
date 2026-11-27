package org.ptit.meeting.modules.notification.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.ptit.meeting.modules.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository
    extends JpaRepository<NotificationPreference, String> {

  Optional<NotificationPreference> findByUserId(Long userId);

  List<NotificationPreference> findAllByUserIdIn(Collection<Long> userIds);
}
