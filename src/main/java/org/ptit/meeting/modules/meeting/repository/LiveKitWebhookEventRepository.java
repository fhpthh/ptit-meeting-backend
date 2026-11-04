package org.ptit.meeting.modules.meeting.repository;

import java.util.Optional;
import org.ptit.meeting.modules.meeting.entity.LiveKitWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface LiveKitWebhookEventRepository
    extends JpaRepository<LiveKitWebhookEvent, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<LiveKitWebhookEvent> findByProviderEventId(String providerEventId);
}
