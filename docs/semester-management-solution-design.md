# ĐẶC TẢ GIẢI PHÁP PHÂN HỆ QUẢN LÝ HỌC KỲ (SEMESTER MANAGEMENT)
## BUSINESS ANALYSIS & SYSTEM SOLUTION DESIGN DOCUMENT

> **Hệ thống:** PTIT Meeting — Nền tảng Phòng học số và Hội họp trực tuyến Học viện  
> **Phân hệ:** Quản lý Đào tạo & Thời khóa biểu (`org.ptit.meeting.modules.timetable`)  
> **Tính năng:** Quản lý Học kỳ & Năm học (Semester Management)  
> **Phiên bản tài liệu:** 1.0  
> **Đối tượng:** Kỹ sư phát triển Backend, Frontend, Tester (QA/QC), Solution Architect  

---

## MỤC LỤC
1. [Bối cảnh Nghiệp vụ & Tầm quan trọng](#1-bối-cảnh-nghiệp-vụ--tầm-quan-trọng)
2. [Các Tác nhân & Phân quyền (Actors & Permissions)](#2-các-tác-nhân--phân-quyền)
3. [Mô hình Dữ liệu (Data Model)](#3-mô-hình-dữ-liệu)
4. [Kiến trúc Triển khai & Nguyên tắc Thiết kế (SOLID & DRY)](#4-kiến-trúc-triển-khai--nguyên-tắc-thiết-kế)
5. [Đặc tả Chi tiết Giao diện API (REST API Specification)](#5-đặc-tả-chi-tiết-giao-diện-api)
6. [Quy tắc Nghiệp vụ Cốt lõi (Core Business Rules)](#6-quy-tắc-nghiệp-vụ-cốt-lõi)
7. [Kế hoạch Kiểm thử & Hướng dẫn Postman](#7-kế-hoạch-kiểm-thử--hướng-dẫn-postman)

---

## 1. BỐI CẢNH NGHIỆP VỤ & TẦM QUAN TRỌNG

### 1.1. Tại sao Quản lý Học kỳ là viên gạch nền móng?
Trong hệ thống đào tạo tín chỉ và nền tảng phòng học số **PTIT Meeting**:
1. **Khóa ngoại liên kết Thời khóa biểu tổng:** Khi Cán bộ Giáo vụ upload file Excel Thời khóa biểu (`/api/v1/admin/timetable/master/upload`), hệ thống bắt buộc phải liên kết với một `semesterId` hợp lệ.
2. **"Tọa độ mốc" cho thuật toán sinh Buổi học số (`meeting_sessions`):**
   * File Excel của PTIT chỉ chứa các cột tuần học tương đối ($1, 2, 3... 17$) và Thứ trong tuần ($2, 3... 8$). File không chứa ngày dương lịch cụ thể của từng buổi.
   * Để sinh chính xác ngày học thực tế (`study_date`) và giờ học (`scheduled_start`, `scheduled_end`), bộ tính toán `ScheduleDateCalculator` phụ thuộc sống còn vào **`semesters.start_date`** (Ngày Thứ Hai đầu tiên của Tuần 1 học kỳ).
   * Nếu `start_date` sai, toàn bộ lịch học của hàng chục nghìn sinh viên và giảng viên sẽ bị lệch ngày trên hệ thống.
3. **Cờ Học kỳ Hiện tại (`is_active`):**
   * Giúp hệ thống tự động nhận diện học kỳ đang diễn ra để hiển thị mặc định TKB cho Sinh viên và Giảng viên khi đăng nhập mà không cần bắt người dùng chọn thủ công học kỳ mỗi lần.
   * **Nguyên tắc nghiệp vụ độc quyền:** Tại một thời điểm, toàn trường chỉ có **duy nhất 1 học kỳ** được kích hoạt (`is_active = true`).

---

## 2. CÁC TÁC NHÂN & PHÂN QUYỀN

| Vai trò (Role) | Mã quyền trong hệ thống | Quyền hạn đối với Học kỳ |
| :--- | :--- | :--- |
| **Cán bộ Phòng Giáo vụ** | `ROLE_STAFF` | Tạo mới, cập nhật, kích hoạt học kỳ hiện tại, xem danh sách. |
| **Quản trị viên hệ thống** | `ROLE_ADMIN` | Toàn quyền quản trị học kỳ. |
| **Giảng viên** | `ROLE_LECTURER` | Chỉ đọc: Xem danh sách học kỳ có lớp dạy (`/api/v1/lecturer/timetable/semesters`). |
| **Sinh viên** | `ROLE_STUDENT` | Chỉ đọc: Xem danh sách học kỳ có đăng ký môn (`/api/v1/student/timetable/semesters`). |

> Cấu hình bảo mật tập trung tại `SecurityConfig.java`:  
> `.requestMatchers("/api/v1/admin/**").hasAnyRole("STAFF", "ADMIN")`

---

## 3. MÔ HÌNH DỮ LIỆU

Bảng `semesters` trong CSDL MySQL:

```sql
CREATE TABLE IF NOT EXISTS semesters (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    code          VARCHAR(50)  NOT NULL UNIQUE,      -- Mã học kỳ: 2026-2027-HK1
    name          VARCHAR(100) NOT NULL,             -- Tên hiển thị: Học kỳ 1 năm học 2026 - 2027
    academic_year VARCHAR(20)  NOT NULL,             -- Năm học: 2026-2027
    semester_num  TINYINT      NOT NULL,             -- Số HK: 1, 2, 3 (HK hè)
    start_date    DATE         NOT NULL,             -- Ngày mốc Thứ Hai Tuần 1
    end_date      DATE         NOT NULL,             -- Ngày kết thúc kỳ học
    is_active     BOOLEAN      NOT NULL DEFAULT FALSE, -- Cờ HK hiện tại
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_semesters_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 4. KIẾN TRÚC TRIỂN KHAI & NGUYÊN TẮC THIẾT KẾ

Tuân thủ nghiêm ngặt **Package-by-Feature** và kiến trúc 3 tầng chuẩn của dự án:

```
src/main/java/org/ptit/meeting/modules/timetable/
├── constant/
│   └── TimetableErrorConstants.java           <-- Khai báo error keys
├── controller/
│   └── SemesterAdminController.java           <-- Chỉ xử lý HTTP & Validation
├── dto/
│   ├── request/
│   │   ├── SemesterCreateRequest.java         <-- DTO tạo mới + Jakarta Validation
│   │   ├── SemesterUpdateRequest.java         <-- DTO cập nhật + Jakarta Validation
│   │   └── SemesterSearchRequest.java         <-- Kế thừa PageRequestDTO cho search & phân trang
│   └── response/
│       └── SemesterResponse.java              <-- Trả dữ liệu công khai + totalWeeks
├── entity/
│   └── Semester.java                          <-- JPA Entity ánh xạ bảng semesters
├── mapper/
│   └── SemesterMapper.java                    <-- Chuyển đổi Entity <-> DTO + tính totalWeeks
├── repository/
│   └── SemesterRepository.java                <-- Thao tác DB + Query Exclusive Active
└── service/
    ├── SemesterAdminService.java              <-- Interface định nghĩa use cases
    └── impl/
        └── SemesterAdminServiceImpl.java      <-- Business logic, @Transactional, validation
```

### Các nguyên tắc cốt lõi áp dụng:
1. **Single Responsibility (SRP):**
   * `SemesterAdminController`: Tiếp nhận HTTP request, gọi Service, bọc kết quả vào `BaseResponse`.
   * `SemesterAdminServiceImpl`: Xử lý toàn bộ logic nghiệp vụ (kiểm tra trùng mã, kiểm tra ngày tháng, reset cờ `isActive`).
   * `SemesterMapper`: Thuần túy chuyển đổi dữ liệu và gọi `ScheduleDateCalculator` để tính tổng số tuần học.
2. **Open / Closed & DRY:**
   * Tái sử dụng `ScheduleDateCalculator` để tính `totalWeeks`.
   * Tái sử dụng `PageRequestDTO` và `PageResponseDTO` cho việc phân trang.
   * Tái sử dụng hệ thống i18n (`messages.properties`, `messages_vi.properties`).
3. **No Hard-coding:**
   * Mọi message lỗi, message thành công và message validation đều được định nghĩa tập trung qua message bundle i18n.
   * Mã lỗi nghiệp vụ quản lý qua `TimetableErrorConstants`.

---

## 5. ĐẶC TẢ CHI TIẾT GIAO DIỆN API

Tất cả các API yêu cầu Header:
```http
Authorization: Bearer <access_token>
```

### 5.1. Tạo mới Học kỳ
* **URL:** `POST /api/v1/admin/semesters`
* **Quyền:** `ROLE_STAFF`, `ROLE_ADMIN`
* **Request Body:**
```json
{
  "code": "2026-2027-HK1",
  "name": "Học kỳ 1 năm học 2026 - 2027",
  "academicYear": "2026-2027",
  "semesterNum": 1,
  "startDate": "2026-08-10",
  "endDate": "2026-12-31",
  "isActive": true
}
```
* **Response (HTTP 201 Created):**
```json
{
  "code": "0",
  "strCode": "success",
  "message": "Tạo mới học kỳ thành công",
  "content": {
    "id": 1,
    "code": "2026-2027-HK1",
    "name": "Học kỳ 1 năm học 2026 - 2027",
    "academicYear": "2026-2027",
    "semesterNum": 1,
    "startDate": "2026-08-10",
    "endDate": "2026-12-31",
    "isActive": true,
    "totalWeeks": 21,
    "createdAt": "2026-11-02T14:30:00",
    "updatedAt": "2026-11-02T14:30:00"
  }
}
```

### 5.2. Cập nhật Học kỳ
* **URL:** `PUT /api/v1/admin/semesters/{id}`
* **Quyền:** `ROLE_STAFF`, `ROLE_ADMIN`
* **Request Body:**
```json
{
  "code": "2026-2027-HK1",
  "name": "Học kỳ 1 năm học 2026 - 2027 (Cập nhật)",
  "academicYear": "2026-2027",
  "semesterNum": 1,
  "startDate": "2026-08-10",
  "endDate": "2026-12-31",
  "isActive": true
}
```
* **Response (HTTP 200 OK):** Trả về thông tin học kỳ sau cập nhật.

### 5.3. Kích hoạt Học kỳ Hiện tại (Exclusive Activation)
* **URL:** `PATCH /api/v1/admin/semesters/{id}/active`
* **Quyền:** `ROLE_STAFF`, `ROLE_ADMIN`
* **Mô tả:** Đặt học kỳ `{id}` thành `isActive = true`, đồng thời tự động chuyển tất cả các học kỳ khác về `isActive = false`.
* **Response (HTTP 200 OK):**
```json
{
  "code": "0",
  "strCode": "success",
  "message": "Kích hoạt học kỳ thành công",
  "content": {
    "id": 1,
    "code": "2026-2027-HK1",
    "isActive": true
  }
}
```

### 5.4. Xem chi tiết Học kỳ
* **URL:** `GET /api/v1/admin/semesters/{id}`
* **Response (HTTP 200 OK):** Chi tiết học kỳ kèm `totalWeeks`.

### 5.5. Lấy thông tin Học kỳ Hiện tại đang Active
* **URL:** `GET /api/v1/admin/semesters/active`
* **Response (HTTP 200 OK):** Thông tin học kỳ có `isActive = true`.

### 5.6. Danh sách & Tìm kiếm Học kỳ (Phân trang)
* **URL:** `GET /api/v1/admin/semesters?keyword=2026&academicYear=2026-2027&page=0&size=10&sortBy=startDate&direction=DESC`
* **Response (HTTP 200 OK):**
```json
{
  "code": "0",
  "strCode": "success",
  "message": "Lấy danh sách học kỳ thành công",
  "content": {
    "content": [
      {
        "id": 1,
        "code": "2026-2027-HK1",
        "name": "Học kỳ 1 năm học 2026 - 2027",
        "academicYear": "2026-2027",
        "semesterNum": 1,
        "startDate": "2026-08-10",
        "endDate": "2026-12-31",
        "isActive": true,
        "totalWeeks": 21
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

---

## 6. QUY TẮC NGHIỆP VỤ CỐT LÕI (CORE BUSINESS RULES)

1. **Ràng buộc Tính duy nhất của Mã học kỳ (`code`):**
   * Khi tạo mới: `existsByCode(code) == true` $\rightarrow$ Ném lỗi `error.timetable.semester.code-existed` (HTTP 409).
   * Khi cập nhật: `existsByCodeAndIdNot(code, id) == true` $\rightarrow$ Ném lỗi `error.timetable.semester.code-existed`.
2. **Ràng buộc Khoảng thời gian Học kỳ:**
   * `endDate` bắt buộc phải sau `startDate` (`endDate.isAfter(startDate)`).
   * Nếu vi phạm $\rightarrow$ Ném lỗi `error.timetable.semester.date-invalid` (HTTP 400).
3. **Ràng buộc Ngày bắt đầu Tuần 1 (`startDate`):**
   * Theo quy chuẩn đào tạo PTIT, tuần học bắt đầu từ Thứ Hai.
   * `startDate.getDayOfWeek() == DayOfWeek.MONDAY`. Nếu người dùng chọn ngày khác Thứ Hai $\rightarrow$ Ném lỗi `error.timetable.semester.start-date-not-monday` (HTTP 400).
4. **Cơ chế Kích hoạt Độc quyền (Exclusive Active Flag):**
   * Khi tạo mới hoặc cập nhật một học kỳ với `isActive = true`, hoặc khi gọi API kích hoạt:
   * Service thực thi query atomic: `semesterRepository.deactivateAll()` để chuyển tất cả các học kỳ cũ về `false`, sau đó mới kích hoạt học kỳ chỉ định.

---

## 7. BẢNG MÃ LỖI NGHIỆP VỤ (TimetableErrorConstants)

| Mã lỗi (`strCode`) | HTTP Status | Thông điệp Tiếng Việt |
| :--- | :---: | :--- |
| `error.timetable.semester.not-found` | 404 | Học kỳ không tồn tại |
| `error.timetable.semester.code-existed` | 409 | Mã học kỳ đã tồn tại trên hệ thống |
| `error.timetable.semester.date-invalid` | 400 | Ngày kết thúc phải diễn ra sau ngày bắt đầu học kỳ |
| `error.timetable.semester.start-date-not-monday` | 400 | Ngày bắt đầu học kỳ phải là ngày Thứ Hai |
