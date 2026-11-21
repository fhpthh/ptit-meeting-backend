CREATE TABLE IF NOT EXISTS ai_realtime_sessions (
    id VARCHAR(36) NOT NULL,
    runtime_session_id VARCHAR(36) NOT NULL,
    enable_epoch BIGINT NOT NULL,
    requested_by_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMP(6) NULL,
    stopped_at TIMESTAMP(6) NULL,
    failure_detail VARCHAR(500) NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_realtime_runtime_session UNIQUE (runtime_session_id),
    CONSTRAINT fk_ai_realtime_runtime_session
      FOREIGN KEY (runtime_session_id) REFERENCES meeting_sessions(public_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ai_final_captions (
    id VARCHAR(36) NOT NULL,
    runtime_session_id VARCHAR(36) NOT NULL,
    stream_id VARCHAR(256) NOT NULL,
    generation BIGINT NOT NULL,
    segment_id VARCHAR(256) NOT NULL,
    language VARCHAR(2) NOT NULL,
    revision BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    text VARCHAR(8000) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_final_caption_revision UNIQUE
      (runtime_session_id, stream_id, generation, segment_id, language, revision),
    CONSTRAINT fk_ai_final_caption_runtime_session
      FOREIGN KEY (runtime_session_id) REFERENCES meeting_sessions(public_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
