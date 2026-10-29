SET NAMES utf8mb4;

START TRANSACTION;

-- Test account: GV.NTTNGA / 123456
INSERT INTO users (
    email,
    password_hash,
    full_name,
    code,
    account_type,
    department,
    status,
    must_change_password,
    created_at,
    updated_at
)
SELECT
    'nguyenthithanhnga@ptit.edu.vn',
    '$2a$10$Q1BlgUgVi5t49QLd2Zva3OWSJJeJiApHXi0SrqnE0VxMOde7yLxEW',
    'Nguyễn Thị Thanh Nga',
    'GV.NTTNGA',
    'LECTURER',
    'QT1 - Quản trị',
    'ACTIVE',
    FALSE,
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE code = 'GV.NTTNGA'
       OR email = 'nguyenthithanhnga@ptit.edu.vn'
);

SET @lecturer_id = (
    SELECT id
    FROM users
    WHERE code = 'GV.NTTNGA'
       OR email = 'nguyenthithanhnga@ptit.edu.vn'
    ORDER BY id
    LIMIT 1
);

UPDATE users
SET email = 'nguyenthithanhnga@ptit.edu.vn',
    password_hash = '$2a$10$Q1BlgUgVi5t49QLd2Zva3OWSJJeJiApHXi0SrqnE0VxMOde7yLxEW',
    full_name = 'Nguyễn Thị Thanh Nga',
    code = 'GV.NTTNGA',
    account_type = 'LECTURER',
    department = 'QT1 - Quản trị',
    status = 'ACTIVE',
    must_change_password = FALSE,
    updated_at = NOW(6)
WHERE id = @lecturer_id;

SET @lecturer_role_id = (
    SELECT id
    FROM roles
    WHERE code = 'ROLE_LECTURER'
    LIMIT 1
);

INSERT INTO user_roles (assigned_at, role_id, user_id)
SELECT NOW(6), @lecturer_role_id, @lecturer_id
WHERE @lecturer_role_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM user_roles
      WHERE user_id = @lecturer_id
        AND role_id = @lecturer_role_id
  );

-- Link only the exact lecturer imported from the 2026-2027 timetable.
UPDATE class_sections
SET lecturer_id = @lecturer_id,
    updated_at = NOW(6)
WHERE semester_id = 1
  AND lecturer_name = 'Nguyễn Thị Thanh Nga';

-- Restore start/end time from Excel periods, then make this lecturer's
-- schedules online for end-to-end testing. The conversion follows the same
-- period mapping used by MasterTimetableExcelAdapter.
UPDATE class_schedules schedule
JOIN class_sections section
  ON section.id = schedule.class_section_id
SET schedule.start_time = CASE schedule.start_period
        WHEN 1 THEN '07:00:00'
        WHEN 2 THEN '07:50:00'
        WHEN 3 THEN '08:50:00'
        WHEN 4 THEN '09:40:00'
        WHEN 5 THEN '10:30:00'
        WHEN 6 THEN '11:20:00'
        WHEN 7 THEN '12:30:00'
        WHEN 8 THEN '13:20:00'
        WHEN 9 THEN '14:20:00'
        WHEN 10 THEN '15:10:00'
        WHEN 11 THEN '16:00:00'
        WHEN 12 THEN '16:50:00'
        ELSE schedule.start_time
    END,
    schedule.end_time = ADDTIME(
        CASE schedule.start_period
            WHEN 1 THEN '07:00:00'
            WHEN 2 THEN '07:50:00'
            WHEN 3 THEN '08:50:00'
            WHEN 4 THEN '09:40:00'
            WHEN 5 THEN '10:30:00'
            WHEN 6 THEN '11:20:00'
            WHEN 7 THEN '12:30:00'
            WHEN 8 THEN '13:20:00'
            WHEN 9 THEN '14:20:00'
            WHEN 10 THEN '15:10:00'
            WHEN 11 THEN '16:00:00'
            WHEN 12 THEN '16:50:00'
            ELSE schedule.start_time
        END,
        SEC_TO_TIME(schedule.num_periods * 50 * 60)
    ),
    schedule.room_text = 'Online',
    schedule.building = NULL,
    schedule.is_online = TRUE,
    schedule.updated_at = NOW(6)
WHERE section.semester_id = 1
  AND section.lecturer_id = @lecturer_id;

-- Every lecturer class section has one reusable online room.
INSERT INTO rooms (
    class_section_id,
    created_by,
    name,
    external_engine_id,
    passcode,
    status,
    created_at,
    updated_at
)
SELECT
    section.id,
    @lecturer_id,
    CONCAT('Phòng trực tuyến - ', section.class_code),
    NULL,
    LPAD(MOD(section.id * 7919, 1000000), 6, '0'),
    'ACTIVE',
    NOW(6),
    NOW(6)
FROM class_sections section
WHERE section.semester_id = 1
  AND section.lecturer_id = @lecturer_id
  AND NOT EXISTS (
      SELECT 1
      FROM rooms room
      WHERE room.class_section_id = section.id
  );

UPDATE rooms room
JOIN class_sections section
  ON section.id = room.class_section_id
SET room.created_by = @lecturer_id,
    room.name = CONCAT('Phòng trực tuyến - ', section.class_code),
    room.status = 'ACTIVE',
    room.updated_at = NOW(6)
WHERE section.semester_id = 1
  AND section.lecturer_id = @lecturer_id;

-- Rebuild only this lecturer's generated sessions to remove stale manual
-- test times and guarantee consistency with class_schedules/weeks_pattern.
DELETE session
FROM meeting_sessions session
JOIN class_schedules schedule
  ON schedule.id = session.schedule_id
JOIN class_sections section
  ON section.id = schedule.class_section_id
WHERE section.semester_id = 1
  AND section.lecturer_id = @lecturer_id;

INSERT INTO meeting_sessions (
    room_id,
    schedule_id,
    study_date,
    scheduled_start,
    scheduled_end,
    actual_start,
    actual_end,
    status,
    created_at,
    updated_at
)
SELECT
    room.id,
    schedule.id,
    DATE_ADD(
        semester.start_date,
        INTERVAL ((week_data.week_number - 1) * 7 + schedule.day_of_week - 2) DAY
    ) AS study_date,
    TIMESTAMP(
        DATE_ADD(
            semester.start_date,
            INTERVAL ((week_data.week_number - 1) * 7 + schedule.day_of_week - 2) DAY
        ),
        schedule.start_time
    ) AS scheduled_start,
    TIMESTAMP(
        DATE_ADD(
            semester.start_date,
            INTERVAL ((week_data.week_number - 1) * 7 + schedule.day_of_week - 2) DAY
        ),
        schedule.end_time
    ) AS scheduled_end,
    NULL,
    NULL,
    'SCHEDULED',
    NOW(6),
    NOW(6)
FROM class_schedules schedule
JOIN class_sections section
  ON section.id = schedule.class_section_id
JOIN semesters semester
  ON semester.id = section.semester_id
JOIN rooms room
  ON room.class_section_id = section.id
JOIN JSON_TABLE(
    CAST(schedule.weeks_pattern AS JSON),
    '$[*]' COLUMNS (
        week_number INT PATH '$'
    )
) AS week_data
WHERE section.semester_id = 1
  AND section.lecturer_id = @lecturer_id
  AND schedule.is_online = TRUE;

COMMIT;
