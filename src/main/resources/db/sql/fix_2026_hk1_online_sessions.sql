-- One-off data correction for the 2026_HK1 workbook imported from
-- 1606_TKB-hoc-ky-1-nam-hoc-2026-2027 (1).xlsx.
-- Scope: semester metadata and ONLINE schedules only. Offline schedules are untouched.

CREATE TABLE IF NOT EXISTS backup_semesters_20261029 LIKE semesters;
INSERT IGNORE INTO backup_semesters_20261029
SELECT * FROM semesters WHERE code = '2026_HK1';

CREATE TABLE IF NOT EXISTS backup_online_schedules_20261029 LIKE class_schedules;
INSERT IGNORE INTO backup_online_schedules_20261029
SELECT schedule.*
FROM class_schedules schedule
JOIN class_sections section ON section.id = schedule.class_section_id
JOIN semesters semester ON semester.id = section.semester_id
WHERE semester.code = '2026_HK1'
  AND schedule.is_online = TRUE;

CREATE TABLE IF NOT EXISTS backup_online_sessions_20261029 LIKE meeting_sessions;
INSERT IGNORE INTO backup_online_sessions_20261029
SELECT session.*
FROM meeting_sessions session
JOIN class_schedules schedule ON schedule.id = session.schedule_id
JOIN class_sections section ON section.id = schedule.class_section_id
JOIN semesters semester ON semester.id = section.semester_id
WHERE semester.code = '2026_HK1'
  AND schedule.is_online = TRUE;

START TRANSACTION;

UPDATE semesters
SET start_date = '2026-08-10'
WHERE code = '2026_HK1';

DELETE session
FROM meeting_sessions session
JOIN class_schedules schedule ON schedule.id = session.schedule_id
JOIN class_sections section ON section.id = schedule.class_section_id
JOIN semesters semester ON semester.id = section.semester_id
WHERE semester.code = '2026_HK1'
  AND schedule.is_online = TRUE;

INSERT INTO meeting_sessions (
    room_id,
    schedule_id,
    study_date,
    scheduled_start,
    scheduled_end,
    status,
    created_at,
    updated_at
)
SELECT
    room.id,
    schedule.id,
    DATE_ADD(
        DATE_ADD(semester.start_date, INTERVAL (week_data.week_number - 1) WEEK),
        INTERVAL (schedule.day_of_week - 2) DAY
    ) AS study_date,
    TIMESTAMP(
        DATE_ADD(
            DATE_ADD(semester.start_date, INTERVAL (week_data.week_number - 1) WEEK),
            INTERVAL (schedule.day_of_week - 2) DAY
        ),
        schedule.start_time
    ) AS scheduled_start,
    TIMESTAMP(
        DATE_ADD(
            DATE_ADD(semester.start_date, INTERVAL (week_data.week_number - 1) WEEK),
            INTERVAL (schedule.day_of_week - 2) DAY
        ),
        schedule.end_time
    ) AS scheduled_end,
    'SCHEDULED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM class_schedules schedule
JOIN class_sections section ON section.id = schedule.class_section_id
JOIN semesters semester ON semester.id = section.semester_id
JOIN rooms room ON room.class_section_id = section.id
JOIN JSON_TABLE(
    schedule.weeks_pattern,
    '$[*]' COLUMNS (week_number INT PATH '$')
) AS week_data
WHERE semester.code = '2026_HK1'
  AND schedule.is_online = TRUE;

COMMIT;
