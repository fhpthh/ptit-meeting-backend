-- MySQL 8. Stop application writes and back up the database before running this file.
-- Run against the selected application database, before starting the new application.
-- Existing TKB ids/times/statuses and existing meeting UUIDs are preserved.
-- Legacy meeting_rooms/meeting_runtime_sessions are retained as migration backups.
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS meeting_merge_storage;
DROP PROCEDURE IF EXISTS meeting_merge_retarget_foreign_keys;
DROP PROCEDURE IF EXISTS meeting_merge_add_index;
DROP PROCEDURE IF EXISTS meeting_merge_add_column;

DELIMITER $$

CREATE PROCEDURE meeting_merge_add_column(IN table_name_arg VARCHAR(64),
    IN column_name_arg VARCHAR(64), IN definition_arg TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = table_name_arg
          AND column_name = column_name_arg) THEN
        SET @merge_sql = CONCAT('ALTER TABLE `', table_name_arg, '` ADD COLUMN `',
            column_name_arg, '` ', definition_arg);
        PREPARE merge_statement FROM @merge_sql;
        EXECUTE merge_statement;
        DEALLOCATE PREPARE merge_statement;
    END IF;
END$$

CREATE PROCEDURE meeting_merge_add_index(IN table_name_arg VARCHAR(64),
    IN index_name_arg VARCHAR(64), IN column_name_arg VARCHAR(64))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = table_name_arg
          AND index_name = index_name_arg) THEN
        SET @merge_sql = CONCAT('ALTER TABLE `', table_name_arg,
            '` ADD UNIQUE INDEX `', index_name_arg, '` (`', column_name_arg, '`)');
        PREPARE merge_statement FROM @merge_sql;
        EXECUTE merge_statement;
        DEALLOCATE PREPARE merge_statement;
    END IF;
END$$

CREATE PROCEDURE meeting_merge_retarget_foreign_keys()
BEGIN
    DECLARE finished INT DEFAULT 0;
    DECLARE child_table VARCHAR(64);
    DECLARE child_column VARCHAR(64);
    DECLARE foreign_key_name VARCHAR(64);
    DECLARE parent_table VARCHAR(64);
    DECLARE delete_rule_arg VARCHAR(30);
    DECLARE update_rule_arg VARCHAR(30);
    DECLARE nullable_arg VARCHAR(3);
    DECLARE legacy_keys CURSOR FOR
        SELECT k.table_name, k.column_name, k.constraint_name, k.referenced_table_name,
            r.delete_rule, r.update_rule, c.is_nullable
        FROM information_schema.key_column_usage k
        JOIN information_schema.referential_constraints r
          ON r.constraint_schema = k.constraint_schema AND r.constraint_name = k.constraint_name
          AND r.table_name = k.table_name
        JOIN information_schema.columns c
          ON c.table_schema = k.table_schema AND c.table_name = k.table_name
          AND c.column_name = k.column_name
        WHERE k.table_schema = DATABASE()
          AND k.referenced_table_name IN ('meeting_rooms', 'meeting_runtime_sessions');
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = 1;

    OPEN legacy_keys;
    retarget_loop: LOOP
        FETCH legacy_keys INTO child_table, child_column, foreign_key_name, parent_table,
            delete_rule_arg, update_rule_arg, nullable_arg;
        IF finished = 1 THEN LEAVE retarget_loop; END IF;

        SET @merge_sql = CONCAT('ALTER TABLE `', child_table, '` DROP FOREIGN KEY `',
            foreign_key_name, '`, MODIFY COLUMN `', child_column,
            '` VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci ',
            IF(nullable_arg = 'YES', 'NULL', 'NOT NULL'), ', ADD CONSTRAINT `',
            LEFT(foreign_key_name, 56), '_unified` FOREIGN KEY (`', child_column, '`) REFERENCES `',
            IF(parent_table = 'meeting_rooms', 'rooms', 'meeting_sessions'),
            '` (`public_id`) ON DELETE ', delete_rule_arg, ' ON UPDATE ', update_rule_arg);
        PREPARE merge_statement FROM @merge_sql;
        EXECUTE merge_statement;
        DEALLOCATE PREPARE merge_statement;
    END LOOP;
    CLOSE legacy_keys;
END$$

CREATE PROCEDURE meeting_merge_storage()
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.key_column_usage
        WHERE table_schema = DATABASE()
          AND referenced_table_name IN ('meeting_rooms', 'meeting_runtime_sessions')
          AND (ordinal_position > 1 OR referenced_column_name <> 'id')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unsupported legacy foreign key; inspect before migration';
    END IF;

    CALL meeting_merge_add_column('rooms', 'public_id',
        'VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL');
    CALL meeting_merge_add_column('rooms', 'code', 'VARCHAR(50) NULL');
    CALL meeting_merge_add_column('rooms', 'description', 'VARCHAR(500) NULL');
    CALL meeting_merge_add_column('rooms', 'room_type', 'VARCHAR(30) NOT NULL DEFAULT ''CLASS''');
    CALL meeting_merge_add_column('meeting_sessions', 'public_id',
        'VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL');
    CALL meeting_merge_add_column('meeting_sessions', 'title', 'VARCHAR(150) NOT NULL DEFAULT ''Online class''');
    CALL meeting_merge_add_column('meeting_sessions', 'provider_room_name', 'VARCHAR(50) NULL');
    CALL meeting_merge_add_column('meeting_sessions', 'schedule_occurrence_id', 'VARCHAR(100) NULL');
    CALL meeting_merge_add_column('meeting_sessions', 'created_by_user_id', 'BIGINT NULL');
    CALL meeting_merge_add_column('meeting_sessions', 'row_version', 'BIGINT NOT NULL DEFAULT 0');

    -- Backfill technical identifiers only; do not change imported schedules or ownership.
    UPDATE rooms SET public_id = UUID(), updated_at = updated_at WHERE public_id IS NULL;
    UPDATE rooms SET code = CONCAT('ROOM-', UPPER(REPLACE(public_id, '-', ''))), updated_at = updated_at
        WHERE code IS NULL;
    UPDATE meeting_sessions SET public_id = UUID(), updated_at = updated_at WHERE public_id IS NULL;
    UPDATE meeting_sessions SET provider_room_name = CONCAT('runtime-session-', REPLACE(public_id, '-', ''))
        , updated_at = updated_at
        WHERE provider_room_name IS NULL;

    ALTER TABLE rooms MODIFY public_id VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
        MODIFY code VARCHAR(50) NOT NULL,
        MODIFY created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
        MODIFY updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
    ALTER TABLE meeting_sessions
        MODIFY public_id VARCHAR(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
        MODIFY provider_room_name VARCHAR(50) NOT NULL,
        MODIFY schedule_id BIGINT NULL,
        MODIFY scheduled_start DATETIME(6) NOT NULL,
        MODIFY scheduled_end DATETIME(6) NOT NULL,
        MODIFY actual_start DATETIME(6) NULL,
        MODIFY actual_end DATETIME(6) NULL,
        MODIFY created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
        MODIFY updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6);
    CALL meeting_merge_add_index('rooms', 'uk_rooms_public_id', 'public_id');
    CALL meeting_merge_add_index('rooms', 'uk_rooms_code', 'code');
    CALL meeting_merge_add_index('meeting_sessions', 'uk_sessions_public_id', 'public_id');
    CALL meeting_merge_add_index('meeting_sessions', 'uk_sessions_provider_room', 'provider_room_name');
    CALL meeting_merge_add_index('meeting_sessions', 'uk_sessions_occurrence', 'schedule_occurrence_id');

    IF EXISTS (SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'meeting_rooms') THEN
        INSERT INTO rooms (public_id, code, name, description, room_type, status, created_by,
            created_at, updated_at)
        SELECT legacy.id, legacy.code, legacy.name, legacy.description, legacy.room_type,
            legacy.status, legacy.created_by_user_id,
            DATE_ADD(legacy.created_at, INTERVAL 7 HOUR), DATE_ADD(legacy.updated_at, INTERVAL 7 HOUR)
        FROM meeting_rooms legacy
        WHERE NOT EXISTS (SELECT 1 FROM rooms room
            WHERE room.public_id = CONVERT(legacy.id USING utf8mb4) COLLATE utf8mb4_unicode_ci);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = 'meeting_runtime_sessions') THEN
        IF EXISTS (SELECT 1 FROM meeting_runtime_sessions legacy
            LEFT JOIN rooms room ON room.public_id = CONVERT(legacy.meeting_room_id USING utf8mb4) COLLATE utf8mb4_unicode_ci
            WHERE room.id IS NULL) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Legacy session has no room; repair before migration';
        END IF;

        INSERT INTO meeting_sessions (public_id, room_id, title, provider_room_name,
            schedule_occurrence_id, study_date, scheduled_start, scheduled_end,
            actual_start, actual_end, status, created_by_user_id, row_version, created_at, updated_at)
        SELECT legacy.id, room.id, legacy.title, legacy.provider_room_name,
            legacy.schedule_occurrence_id, DATE(DATE_ADD(legacy.scheduled_start_at, INTERVAL 7 HOUR)),
            DATE_ADD(legacy.scheduled_start_at, INTERVAL 7 HOUR),
            DATE_ADD(legacy.scheduled_end_at, INTERVAL 7 HOUR),
            DATE_ADD(legacy.actual_start_at, INTERVAL 7 HOUR),
            DATE_ADD(legacy.actual_end_at, INTERVAL 7 HOUR),
            CASE WHEN legacy.status = 'ACTIVE' THEN 'LIVE' ELSE legacy.status END,
            legacy.created_by_user_id, legacy.row_version,
            DATE_ADD(legacy.created_at, INTERVAL 7 HOUR), DATE_ADD(legacy.updated_at, INTERVAL 7 HOUR)
        FROM meeting_runtime_sessions legacy
        JOIN rooms room ON room.public_id = CONVERT(legacy.meeting_room_id USING utf8mb4) COLLATE utf8mb4_unicode_ci
        WHERE NOT EXISTS (SELECT 1 FROM meeting_sessions session
            WHERE session.public_id = CONVERT(legacy.id USING utf8mb4) COLLATE utf8mb4_unicode_ci);
    END IF;

    CALL meeting_merge_retarget_foreign_keys();
END$$

DELIMITER ;

CALL meeting_merge_storage();

DROP PROCEDURE meeting_merge_storage;
DROP PROCEDURE meeting_merge_retarget_foreign_keys;
DROP PROCEDURE meeting_merge_add_index;
DROP PROCEDURE meeting_merge_add_column;
