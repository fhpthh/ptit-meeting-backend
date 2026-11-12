-- Run once when upgrading an existing Egress-based meeting_recordings table.
-- New databases are created directly from the JPA mapping and do not need this script.

ALTER TABLE meeting_recordings
  ADD COLUMN mime_type VARCHAR(100) NULL AFTER object_key,
  ADD COLUMN storage_upload_id VARCHAR(500) NULL AFTER mime_type,
  ADD COLUMN last_activity_at DATETIME(6) NULL AFTER ended_at;

UPDATE meeting_recordings
SET mime_type = CASE
      WHEN object_key LIKE '%.webm' THEN 'video/webm'
      ELSE 'video/mp4'
    END,
    started_at = COALESCE(started_at, created_at, CURRENT_TIMESTAMP(6)),
    last_activity_at = COALESCE(provider_updated_at, updated_at, created_at, CURRENT_TIMESTAMP(6)),
    ended_at = CASE
      WHEN status IN ('STARTING', 'ACTIVE', 'STOPPING')
        THEN COALESCE(ended_at, CURRENT_TIMESTAMP(6))
      ELSE ended_at
    END,
    active_slot = CASE
      WHEN status IN ('STARTING', 'ACTIVE', 'STOPPING') THEN NULL
      ELSE active_slot
    END,
    status = CASE
      WHEN status IN ('STARTING', 'ACTIVE', 'STOPPING') THEN 'ABORTED'
      WHEN status = 'LIMIT_REACHED' THEN 'FAILED'
      ELSE status
    END;

ALTER TABLE meeting_recordings
  MODIFY COLUMN mime_type VARCHAR(100) NOT NULL,
  MODIFY COLUMN started_at DATETIME(6) NOT NULL,
  MODIFY COLUMN last_activity_at DATETIME(6) NOT NULL,
  DROP INDEX uk_meeting_recordings_provider_egress,
  DROP INDEX idx_meeting_recordings_status_updated,
  DROP INDEX idx_meeting_recordings_provider_room,
  DROP COLUMN provider_room_name,
  DROP COLUMN provider_egress_id,
  DROP COLUMN provider_updated_at,
  DROP COLUMN failure_code,
  ADD INDEX idx_meeting_recordings_status_activity (status, last_activity_at);
