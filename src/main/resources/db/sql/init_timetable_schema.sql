SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 1. HỌC KỲ
CREATE TABLE IF NOT EXISTS semesters (
                                         id            BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         code          VARCHAR(50)  NOT NULL UNIQUE,
    name          VARCHAR(100) NOT NULL,
    academic_year VARCHAR(20)  NOT NULL,
    semester_num  TINYINT      NOT NULL,
    start_date    DATE         NOT NULL,
    end_date      DATE         NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_semesters_active (is_active)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. MÔN HỌC
CREATE TABLE IF NOT EXISTS subjects (
                                        id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    credit      INT          NOT NULL DEFAULT 3,
    department  VARCHAR(150) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. LỚP HỌC PHẦN (Đã thêm lecturer_name và cohort)
CREATE TABLE IF NOT EXISTS class_sections (
                                              id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                                              semester_id      BIGINT       NOT NULL,
                                              subject_id       BIGINT       NOT NULL,
                                              lecturer_id      BIGINT       NULL,
                                              lecturer_name    VARCHAR(150) NULL,         -- Tên text gốc của Giảng viên từ Excel
    cohort           VARCHAR(20)  NULL,         -- Khóa học: 2024, 2025
    class_code       VARCHAR(50)  NOT NULL,     -- VD: AI1306_01
    group_number     VARCHAR(20)  NULL,
    practice_group   VARCHAR(20)  NULL,
    management_class VARCHAR(255) NULL,         -- Danh sách lớp QLSV cách nhau dấu phẩy
    status           VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_class_semester (class_code, semester_id),
    INDEX idx_class_lecturer (lecturer_id),
    INDEX idx_class_semester (semester_id),
    CONSTRAINT fk_cs_semester FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE CASCADE,
    CONSTRAINT fk_cs_subject  FOREIGN KEY (subject_id)  REFERENCES subjects(id) ON DELETE RESTRICT,
    CONSTRAINT fk_cs_lecturer FOREIGN KEY (lecturer_id) REFERENCES users(id) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. LỊCH HỌC ĐỊNH KỲ
CREATE TABLE IF NOT EXISTS class_schedules (
                                               id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                                               class_section_id BIGINT       NOT NULL,
                                               day_of_week      TINYINT      NOT NULL,
                                               start_period     INT          NOT NULL,
                                               num_periods      INT          NOT NULL,
                                               start_time       TIME         NOT NULL,
                                               end_time         TIME         NOT NULL,
                                               weeks_pattern    VARCHAR(100) NOT NULL,
    room_text        VARCHAR(50)  NULL,
    building         VARCHAR(50)  NULL,
    is_online        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_schedules_class (class_section_id),
    INDEX idx_schedules_online (is_online),
    CONSTRAINT fk_sched_class FOREIGN KEY (class_section_id) REFERENCES class_sections(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. PHÒNG TRỰC TUYẾN CỐ ĐỊNH
CREATE TABLE IF NOT EXISTS rooms (
                                     id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     class_section_id   BIGINT       NULL UNIQUE,
                                     created_by         BIGINT       NULL,
                                     name               VARCHAR(150) NOT NULL,
    external_engine_id VARCHAR(100) NULL,
    passcode           VARCHAR(20)  NULL,
    status             VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_class FOREIGN KEY (class_section_id) REFERENCES class_sections(id) ON DELETE CASCADE,
    CONSTRAINT fk_room_user  FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. BUỔI HỌC CỤ THỂ
CREATE TABLE IF NOT EXISTS meeting_sessions (
                                                id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                room_id          BIGINT      NOT NULL,
                                                schedule_id      BIGINT      NOT NULL,
                                                study_date       DATE        NOT NULL,
                                                scheduled_start  DATETIME    NOT NULL,
                                                scheduled_end    DATETIME    NOT NULL,
                                                actual_start     DATETIME    NULL,
                                                actual_end       DATETIME    NULL,
                                                status           VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_sessions_lookup (study_date, status),
    INDEX idx_sessions_room (room_id),
    INDEX idx_sessions_schedule (schedule_id),
    CONSTRAINT fk_sess_room  FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_sess_sched FOREIGN KEY (schedule_id) REFERENCES class_schedules(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. GHI DANH SINH VIÊN
CREATE TABLE IF NOT EXISTS class_enrollments (
                                                 id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                 class_section_id BIGINT      NOT NULL,
                                                 student_id       BIGINT      NOT NULL,
                                                 status           VARCHAR(30) NOT NULL DEFAULT 'ENROLLED',
    enrolled_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_enrollment (class_section_id, student_id),
    INDEX idx_enrollment_student (student_id),
    INDEX idx_enrollment_class (class_section_id),
    CONSTRAINT fk_enr_class   FOREIGN KEY (class_section_id) REFERENCES class_sections(id) ON DELETE CASCADE,
    CONSTRAINT fk_enr_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. LỊCH SỬ IMPORT
CREATE TABLE IF NOT EXISTS import_histories (
                                                id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                semester_id  BIGINT       NOT NULL,
                                                uploaded_by  BIGINT       NOT NULL,
                                                file_name    VARCHAR(255) NOT NULL,
    file_url     VARCHAR(500) NULL,
    import_type  VARCHAR(50)  NOT NULL,
    total_rows   INT          NOT NULL DEFAULT 0,
    success_rows INT          NOT NULL DEFAULT 0,
    failed_rows  INT          NOT NULL DEFAULT 0,
    status       VARCHAR(30)  NOT NULL DEFAULT 'PROCESSING',
    error_log    JSON         NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_imp_semester FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE CASCADE,
    CONSTRAINT fk_imp_user     FOREIGN KEY (uploaded_by) REFERENCES users(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;