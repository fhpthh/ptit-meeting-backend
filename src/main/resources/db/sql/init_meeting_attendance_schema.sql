SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS meeting_participant_connections (
  id                   VARCHAR(36)  NOT NULL,
  runtime_session_id   VARCHAR(36)  NOT NULL,
  user_id              BIGINT       NOT NULL,
  participant_identity VARCHAR(128) NOT NULL,
  participant_sid      VARCHAR(128) NOT NULL,
  joined_at            DATETIME(6)  NULL,
  left_at              DATETIME(6)  NULL,
  duration_seconds     BIGINT       NULL,
  status               VARCHAR(30)  NOT NULL,
  disconnect_reason    VARCHAR(100) NULL,
  created_at           DATETIME(6)  NOT NULL,
  updated_at           DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_meeting_connections_session_sid
    UNIQUE (runtime_session_id, participant_sid),
  INDEX idx_meeting_connections_session_user (runtime_session_id, user_id),
  INDEX idx_meeting_connections_session_identity (runtime_session_id, participant_identity),
  INDEX idx_meeting_connections_open (runtime_session_id, left_at),
  CONSTRAINT fk_meeting_connections_runtime_session
    FOREIGN KEY (runtime_session_id) REFERENCES meeting_sessions(public_id)
    ON DELETE CASCADE,
  CONSTRAINT fk_meeting_connections_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS livekit_webhook_events (
  id                  VARCHAR(36)  NOT NULL,
  provider_event_id   VARCHAR(100) NOT NULL,
  event_type          VARCHAR(50)  NOT NULL,
  provider_created_at DATETIME(6)  NOT NULL,
  processing_status   VARCHAR(30)  NOT NULL,
  processed_at        DATETIME(6)  NULL,
  failure_reason      VARCHAR(500) NULL,
  created_at          DATETIME(6)  NOT NULL,
  updated_at          DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_livekit_webhook_event_provider_id UNIQUE (provider_event_id),
  INDEX idx_livekit_webhook_events_status_created (processing_status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
