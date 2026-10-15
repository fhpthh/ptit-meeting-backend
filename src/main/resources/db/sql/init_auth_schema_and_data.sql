-- 1. TẠO CÁC BẢNG XÁC THỰC & PHÂN QUYỀN
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS users (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    email                VARCHAR(150) NOT NULL UNIQUE,
    password_hash        VARCHAR(255) NULL,
    full_name            VARCHAR(150) NOT NULL,
    code                 VARCHAR(30)  NULL,
    account_type         VARCHAR(50)  NOT NULL,
    department           VARCHAR(150) NULL,
    status               VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    must_change_password BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_code (code),
    INDEX idx_users_account_type (account_type),
    INDEX idx_users_login_lookup (email, code, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_identity_providers (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    provider            VARCHAR(50)  NOT NULL DEFAULT 'AZURE_AD',
    provider_subject_id VARCHAR(100) NOT NULL,
    email_at_login      VARCHAR(150) NOT NULL,
    linked_at           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at       DATETIME     NULL,
    UNIQUE KEY uk_provider_subject (provider, provider_subject_id),
    CONSTRAINT fk_identity_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS permissions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    module      VARCHAR(50)  NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_roles (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL,
    role_id     BIGINT   NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS role_permissions (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    CONSTRAINT fk_rp_role       FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. NẠP DỮ LIỆU CÁC VAI TRÒ MẶC ĐỊNH
INSERT IGNORE INTO roles (id, code, name, description) VALUES
(1, 'ROLE_STUDENT', 'Sinh viên', 'Sinh viên tham gia lớp học trực tuyến'),
(2, 'ROLE_LECTURER', 'Giảng viên', 'Giảng viên quản lý lớp học và điểm danh'),
(3, 'ROLE_STAFF', 'Cán bộ', 'Cán bộ quản lý hành chính và sự kiện'),
(4, 'ROLE_ADMIN', 'Quản trị viên', 'Quản trị toàn bộ hệ thống');

-- 3. NẠP TÀI KHOẢN MẪU ĐỂ TEST
-- Mật khẩu mặc định là: Ptit@123 (đã hash chuẩn BCrypt: $2a$10$vH2cBAdgqXZqyFKPhlnn/ug/gzPHR9KKb0FPX2j8eT4cdVKpAKV7K)
INSERT IGNORE INTO users (id, email, password_hash, full_name, code, account_type, department, status, must_change_password) VALUES
(1, 'b22dccn001@stu.ptit.edu.vn', '$2a$10$vH2cBAdgqXZqyFKPhlnn/ug/gzPHR9KKb0FPX2j8eT4cdVKpAKV7K', 'Nguyễn Văn A', 'B22DCCN001', 'STUDENT', 'Khoa CNTT 1', 'ACTIVE', 1),
(2, 'gv001@ptit.edu.vn', '$2a$10$vH2cBAdgqXZqyFKPhlnn/ug/gzPHR9KKb0FPX2j8eT4cdVKpAKV7K', 'Thầy Trần Văn Giảng', 'GV001', 'LECTURER', 'Khoa CNTT 1', 'ACTIVE', 0);

-- Gán quyền cho user mẫu
INSERT IGNORE INTO user_roles (user_id, role_id) VALUES
(1, 1),
(2, 2);

SET FOREIGN_KEY_CHECKS = 1;
