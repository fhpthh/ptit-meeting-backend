-- Human review overlays; AI originals remain immutable. Published snapshot is separate.
CREATE TABLE IF NOT EXISTS recording_subtitle_reviews (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 recording_id VARCHAR(36) NOT NULL,
 draft_id VARCHAR(36) NOT NULL,
 document_json LONGTEXT NOT NULL,
 published_json LONGTEXT NULL,
 revision BIGINT NOT NULL,
 published_revision BIGINT NULL,
 deleted BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP(6) NOT NULL,
 updated_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT uk_recording_subtitle_review UNIQUE(recording_id),
 CONSTRAINT fk_recording_review_draft FOREIGN KEY(draft_id) REFERENCES recording_translation_drafts(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
