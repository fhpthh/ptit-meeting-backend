SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS meeting_session_bans (
  id                 VARCHAR(36)  NOT NULL,
  runtime_session_id VARCHAR(36)  NOT NULL,
  user_id            BIGINT       NOT NULL,
  banned_by_user_id  BIGINT       NOT NULL,
  reason             VARCHAR(500) NULL,
  banned_at          DATETIME(6)  NOT NULL,
  revoked_at         DATETIME(6)  NULL,
  revoked_by_user_id BIGINT       NULL,
  created_at         DATETIME(6)  NOT NULL,
  updated_at         DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_meeting_session_bans_session_user
    UNIQUE (runtime_session_id, user_id),
  INDEX idx_meeting_session_bans_session_active (runtime_session_id, revoked_at),
  INDEX idx_meeting_session_bans_user (user_id),
  CONSTRAINT fk_meeting_session_bans_runtime_session
    FOREIGN KEY (runtime_session_id) REFERENCES meeting_runtime_sessions(id)
    ON DELETE CASCADE,
  CONSTRAINT fk_meeting_session_bans_user
    FOREIGN KEY (user_id) REFERENCES users(id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_meeting_session_bans_banned_by
    FOREIGN KEY (banned_by_user_id) REFERENCES users(id)
    ON DELETE RESTRICT,
  CONSTRAINT fk_meeting_session_bans_revoked_by
    FOREIGN KEY (revoked_by_user_id) REFERENCES users(id)
    ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
