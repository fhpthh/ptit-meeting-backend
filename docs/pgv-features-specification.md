# TÀI LIỆU ĐẶC TẢ TÍNH NĂNG DÀNH CHO PHÒNG GIÁO VỤ (ROLE PGV / ADMIN)
## DÀNH CHO ĐỘI NGŨ FRONTEND (FE) THIẾT KẾ & PHÁT TRIỂN GIAO DIỆN

> **Hệ thống:** PTIT Meeting — Nền tảng Phòng học số và Hội họp trực tuyến Học viện  
> **Phân hệ:** Quản trị Đào tạo & Quản lý Thời khóa biểu (`ROLE_STAFF`, `ROLE_ADMIN`)  
> **Mục đích:** Đặc tả toàn bộ tính năng, luồng nghiệp vụ, giao diện đề xuất (UI/UX) và hợp đồng API (API Contracts) phục vụ đội ngũ Frontend phát triển màn hình cho Cán bộ Phòng Giáo vụ.  
> **Phiên bản tài liệu:** 1.0  

---

## MỤC LỤC
1. [Tổng quan & Phân quyền Phân hệ PGV](#1-tổng-quan--phân-quyền-phân-hệ-pgv)
2. [Sơ đồ Cấu trúc Giao diện Đề xuất cho Frontend (UI Sitemap)](#2-sơ-đồ-cấu-trúc-giao-diện-đề-xuất-cho-frontend-ui-sitemap)
3. [Chi tiết Các Tính năng & Hợp đồng API (API Contracts)](#3-chi-tiết-các-tính-năng--hợp-đồng-api)
   - [3.1. Xác thực & Điều hướng Đăng nhập PGV](#31-xác-thực--điều-hướng-đăng-nhập-pgv)
   - [3.2. Quản lý Học kỳ & Năm học (Semester Management)](#32-quản-lý-học-kỳ--năm-học-semester-management)
   - [3.3. Import Thời khóa biểu tổng (Master Timetable Import)](#33-import-thời-khóa-biểu-tổng-master-timetable-import)
   - [3.4. Import Danh sách Sinh viên vào Lớp học phần (Student Enrollment Import)](#34-import-danh-sách-sinh-viên-vào-lớp-học-phần-student-enrollment-import)
   - [3.5. Lịch sử Import & Đối soát File (Import Histories & Audit)](#35-lịch-sử-import--đối-soát-file-import-histories--audit)
   - [3.6. Tra cứu & Giám sát Thời khóa biểu (Monitoring Timetable)](#36-tra-cứu--giám-sát-thời-khóa-biểu-monitoring-timetable)
4. [Định dạng Dữ liệu & Quy chuẩn Response Chung](#4-định-dạng-dữ-liệu--quy-chuẩn-response-chung)
5. [Bảng Mã Lỗi Frontend Cần Xử lý (Error Handling)](#5-bảng-mã-lỗi-frontend-cần-xử-lý-error-handling)

---

## 1. TỔNG QUAN & PHÂN QUYỀN PHÂN HỆ PGV

### 1.1. Tác nhân
* **Cán bộ Phòng Giáo vụ (`ROLE_STAFF`)**: Người chịu trách nhiệm quản trị danh mục học kỳ, nạp thời khóa biểu học kỳ, nạp danh sách sinh viên các lớp học phần và đối soát lỗi dữ liệu.
* **Quản trị viên Hệ thống (`ROLE_ADMIN`)**: Sở hữu toàn bộ quyền hạn của PGV, đồng thời quản trị người dùng và cấu hình hệ thống.

### 1.2. Cơ chế Bảo mật & Header API
* Mọi API của PGV nằm dưới tiền tố `/api/v1/admin/**`.
* Frontend **bắt buộc** phải gửi JWT Access Token trong header của mỗi request:
  ```http
  Authorization: Bearer <access_token>
  ```
* Nếu token hết hạn (HTTP 401), Frontend tự động gọi API refresh token hoặc điều hướng về `/login`.
* Nếu người dùng không có quyền `ROLE_STAFF` hoặc `ROLE_ADMIN` (HTTP 403), Frontend hiển thị trang *403 Forbidden*.

---

## 2. SƠ ĐỒ CẤU TRÚC GIAO DIỆN ĐỀ XUẤT CHO FRONTEND (UI SITEMAP)

Frontend nên tổ chức Layout Admin/PGV với Sidebar như sau:

```
[Bảng điều khiển PGV - Layout Admin]
├── 1. Quản lý Học kỳ (/admin/semesters)
│   ├── Danh sách học kỳ (Bảng dữ liệu + Tìm kiếm + Phân trang)
│   ├── Modal / Form: Tạo mới học kỳ
│   ├── Modal / Form: Cập nhật học kỳ
│   └── Nút tác vụ: Kích hoạt học kỳ hiện tại (Active)
│
├── 2. Quản lý Thời khóa biểu (/admin/timetable)
│   ├── Tab 1: Import TKB tổng (/admin/timetable/master-import)
│   │   ├── Form chọn Học kỳ + Kéo thả file Excel TKB tổng
│   │   └── Card / Modal kết quả Import (Số môn, lớp, phòng online, buổi học sinh ra, bảng lỗi chi tiết)
│   └── Tab 2: Import Danh sách Sinh viên (/admin/timetable/enrollment-import)
│       ├── Chọn Lớp học phần (Section ID) + Kéo thả file DSSV.xlsx
│       └── Báo cáo kết quả ghi danh (Số SV tạo mới/cập nhật, lỗi từng dòng)
│
├── 3. Lịch sử Import & Đối soát (/admin/timetable/import-histories)
│   ├── Bảng lịch sử (Thời gian, Tên file, Loại import, Người upload, Trạng thái, Số dòng)
│   ├── Nút Tải lại file Excel gốc (.xlsx)
│   └── Drawer / Modal: Xem danh sách lỗi chi tiết từng dòng
│
└── 4. Giám sát Thời khóa biểu (/admin/timetable/preview)
    ├── Xem TKB theo Sinh viên (Nhập MSV để tra cứu lịch tuần)
    └── Xem TKB theo Giảng viên (Chọn Giảng viên để tra cứu lịch dạy)
```

---

## 3. CHI TIẾT CÁC TÍNH NĂNG & HỢP ĐỒNG API

### 3.1. Xác thực & Điều hướng Đăng nhập PGV

#### API 1: Đăng nhập Cán bộ PGV bằng Tài khoản / Mật khẩu
* **Endpoint:** `POST /api/v1/auth/login`
* **Content-Type:** `application/json`
* **Request Body:**
  ```json
  {
    "username": "gv001@ptit.edu.vn",
    "password": "Password@123"
  }
  ```
* **Response Body (`200 OK`):**
  ```json
  {
    "code": "0",
    "strCode": "success",
    "message": "Đăng nhập thành công.",
    "content": {
      "accessToken": "eyJhbGciOi...",
      "refreshToken": "d28333af-...",
      "tokenType": "Bearer",
      "expiresIn": 1800,
      "mustChangePassword": false,
      "user": {
        "id": 1,
        "email": "gv001@ptit.edu.vn",
        "code": "GV001",
        "fullName": "Nguyễn Văn A",
        "accountType": "LECTURER",
        "department": "Phòng Giáo vụ",
        "roles": ["ROLE_STAFF", "ROLE_LECTURER"],
        "permissions": []
      }
    }
  }
  ```
> **FE Lưu ý:** Kiểm tra mảng `user.roles`: nếu chứa `ROLE_STAFF` hoặc `ROLE_ADMIN`, cho phép truy cập menu Quản trị PGV.

#### API 2: Lấy thông tin tài khoản đang đăng nhập
* **Endpoint:** `GET /api/v1/auth/me`
* **Headers:** `Authorization: Bearer <token>`
* **Mục đích:** Khi người dùng F5 tải lại trang, FE gọi API này để khôi phục profile và quyền hạn.

---

### 3.2. Quản lý Học kỳ & Năm học (Semester Management)

#### API 1: Lấy danh sách Học kỳ (Có Tìm kiếm & Phân trang)
* **Endpoint:** `GET /api/v1/admin/semesters`
* **Query Parameters:**
  * `keyword` *(String, optional)*: Tìm kiếm theo mã hoặc tên học kỳ.
  * `academicYear` *(String, optional)*: Lọc theo năm học (VD: `2026-2027`).
  * `page` *(int, default=0)*: Chỉ số trang (bắt đầu từ 0).
  * `size` *(int, default=20)*: Số bản ghi mỗi trang.
  * `sortBy` *(String, default="startDate")*: Trường sắp xếp.
  * `direction` *(String, default="DESC")*: Hướng sắp xếp (`ASC` hoặc `DESC`).
* **Response (`200 OK`):**
  ```json
  {
    "code": "0",
    "strCode": "success",
    "message": "SUCCESSFUL",
    "content": {
      "content": [
        {
          "id": 1,
          "code": "2026-2027-HK1",
          "name": "Học kỳ 1 năm học 2026 - 2027",
          "academicYear": "2026-2027",
          "semesterNum": 1,
          "startDate": "2026-09-07",
          "endDate": "2027-01-17",
          "isActive": true,
          "totalWeeks": 19,
          "classSectionCount": 2846,
          "createdAt": "2026-11-01T10:00:00",
          "updatedAt": "2026-11-01T10:00:00"
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1,
      "last": true
    }
  }
  ```

#### API 2: Tạo mới Học kỳ
* **Endpoint:** `POST /api/v1/admin/semesters`
* **Content-Type:** `application/json`
* **Request Body:**
  ```json
  {
    "code": "2026-2027-HK2",
    "name": "Học kỳ 2 năm học 2026 - 2027",
    "academicYear": "2026-2027",
    "semesterNum": 2,
    "startDate": "2027-02-01",
    "endDate": "2027-06-20",
    "isActive": false
  }
  ```
* **Validation Frontend:**
  * `code`: Bắt buộc, tối đa 50 ký tự, không trùng với học kỳ đã có.
  * `name`: Bắt buộc, tối đa 100 ký tự.
  * `academicYear`: Bắt buộc, định dạng `YYYY-YYYY` (VD: `2026-2027`).
  * `semesterNum`: Số nguyên 1, 2 hoặc 3 (3 = Kỳ hè).
  * `startDate`: Phải là ngày Thứ Hai (ngày mốc bắt đầu tuần 1).
  * `endDate`: Phải sau `startDate`.

#### API 3: Cập nhật thông tin Học kỳ
* **Endpoint:** `PUT /api/v1/admin/semesters/{id}`
* **Request Body:** Tương tự API Tạo mới.

#### API 4: Kích hoạt Học kỳ Hiện tại (Set Active)
* **Endpoint:** `PATCH /api/v1/admin/semesters/{id}/active`
* **Ý nghĩa:** Chuyển học kỳ này thành `isActive = true`. Backend tự động hủy kích hoạt (`isActive = false`) của tất cả học kỳ còn lại.

#### API 5: Lấy Học kỳ Đang Kích hoạt
* **Endpoint:** `GET /api/v1/admin/semesters/active`
* **Mục đích:** FE dùng để chọn sẵn giá trị mặc định cho dropdown chọn Học kỳ trên các màn hình import và xem TKB.

#### API 6: Xóa Học kỳ
* **Endpoint:** `DELETE /api/v1/admin/semesters/{id}`
* **Quy tắc nghiệp vụ:** Chỉ cho phép xóa học kỳ không hoạt động, chưa có lớp học phần và chưa có lịch sử import. Học kỳ đang hoạt động hoặc đã có dữ liệu liên quan sẽ bị từ chối để tránh mất dữ liệu.

---

### 3.3. Import Thời khóa biểu tổng (Master Timetable Import)

Tính năng cốt lõi cho phép nạp toàn bộ file Excel TKB tổng của trường vào học kỳ đã chọn.

#### API: Tải lên File Thời khóa biểu tổng
* **Endpoint:** `POST /api/v1/admin/timetable/master/upload`
* **Content-Type:** `multipart/form-data`
* **Form Data Parameters:**
  * `semesterId` *(Long, required)*: ID của học kỳ cần import.
  * `file` *(MultipartFile, required)*: File Excel `.xlsx` hoặc `.xls`.
* **Cơ chế xử lý tự động của Backend:**
  1. Tự quét dòng tiêu đề động (Header) và dãy số tuần ngang ($1, 2... 17$).
  2. Tạo/cập nhật danh mục môn học (`subjects`).
  3. Tạo lớp học phần (`class_sections`) và lịch học (`class_schedules`).
  4. Cấp phòng học số (`rooms`) kèm mã PIN ngẫu nhiên cho các môn Online (`is_online = true`).
  5. Tính toán chính xác ngày dương lịch (`study_date`) theo `semester.startDate` và tự động sinh các buổi học số (`meeting_sessions`).
* **Response Body (`200 OK`):**
  ```json
  {
    "code": "0",
    "strCode": "success",
    "message": "Import thời khóa biểu tổng thành công",
    "content": {
      "importId": 12,
      "fileName": "1606_TKB-hoc-ky-1-nam-hoc-2026-2027.xlsx",
      "fileSize": "1.2 MB",
      "semesterId": 1,
      "semesterCode": "2026-2027-HK1",
      "semesterName": "Học kỳ 1 năm học 2026 - 2027",
      "status": "SUCCESS",
      "totalRows": 1500,
      "successRows": 1495,
      "failureRows": 5,
      "statistics": {
        "headerRow": 7,
        "dataStartRow": 10,
        "totalWeeksDetected": 17,
        "mappedColumnsCount": 16,
        "subjectsProcessed": 120,
        "classSectionsProcessed": 350,
        "classSchedulesSaved": 420,
        "onlineRoomsAllocated": 85,
        "meetingSessionsGenerated": 1275
      },
      "errors": [
        {
          "rowNumber": 45,
          "errorMessage": "Dòng 45: Giảng viên không hợp lệ"
        }
      ],
      "completedAt": "2026-11-01T14:30:00"
    }
  }
  ```
> **UI Đề xuất:** Hiển thị thẻ Card Thống kê (Dashboard) gồm:
> - Tổng dòng xử lý: `1500` (Thành công: `1495`, Thất bại: `5`)
> - Số phòng online cấp phát: `85`
> - Số buổi học số đã lên lịch: `1275`
> - Nếu `failureRows > 0`: Hiển thị bảng danh sách các dòng bị lỗi để cán bộ tải về xem lại.

---

### 3.4. Import Danh sách Sinh viên vào Lớp học phần (Student Enrollment Import)

Tính năng cho phép PGV tải danh sách sinh viên (`DSSV.xlsx`) vào một Lớp học phần cụ thể (`sectionId`).

#### Cấu trúc File Excel DSSV Mẫu:
* Dòng 1: Tiêu đề cột (`STT`, `MSV`, `Họ và tên`, `Ngày sinh`, `Lớp`).
* Dòng 2 trở đi: Dữ liệu sinh viên (Ví dụ: `1 | B22DCCN356 | Phan Thị Hồng Huế | 22/01/2004 | D22CNPM04`).

#### API: Tải lên Danh sách Sinh viên
* **Endpoint:** `POST /api/v1/admin/timetable/sections/{sectionId}/enrollments/upload`
* **Content-Type:** `multipart/form-data`
* **Path Variable:**
  * `sectionId` *(Long, required)*: ID của lớp học phần cần nạp sinh viên.
* **Form Data Parameters:**
  * `file` *(MultipartFile, required)*: File Excel `.xlsx`.
* **Cơ chế xử lý tự động của Backend:**
  1. Đọc từng dòng: bóc tách MSV chuẩn có `DC` (VD: `B22DCCN356`), Họ tên, Lớp quản lý.
  2. Tạo tài khoản sinh viên nếu chưa có:
     - Username: `B22DCCN356`
     - Mật khẩu mặc định: `Ptit@123` (đã mã hóa BCrypt)
     - Email tự sinh Office 365: `HuePTH.B22CN356@stu.ptit.edu.vn`
     - Cờ: `mustChangePassword = true`, Quyền: `ROLE_STUDENT`
  3. Ghi danh sinh viên vào lớp (`class_enrollments`) với trạng thái `ENROLLED`. Nếu đã ghi danh từ trước thì bỏ qua (Idempotency, không lỗi, không nhân bản).
* **Response Body (`200 OK`):**
  ```json
  {
    "code": "0",
    "strCode": "success",
    "message": "Import danh sách sinh viên thành công",
    "content": {
      "importId": 15,
      "fileName": "DSSV_D22CNPM04.xlsx",
      "fileSize": "45.2 KB",
      "status": "SUCCESS",
      "totalRows": 60,
      "successRows": 60,
      "failureRows": 0,
      "statistics": {
        "classSectionsProcessed": 1
      },
      "errors": [],
      "completedAt": "2026-11-03T10:00:00"
    }
  }
  ```

---

### 3.5. Lịch sử Import & Đối soát File (Import Histories & Audit)

Hệ thống tự động lưu trữ mọi file Excel đã upload lên server và ghi nhận nhật ký chi tiết.

#### API 1: Tra cứu Danh sách Lịch sử Import
* **Endpoint:** `GET /api/v1/admin/timetable/import-histories`
* **Query Parameters:**
  * `semesterId` *(Long, optional)*: Lọc theo học kỳ.
  * `status` *(String, optional)*: `SUCCESS`, `PARTIAL`, `FAILED`.
  * `page`, `size`, `sortBy`, `direction` *(Kế thừa phân trang)*.
* **Response Body (`200 OK`):**
  ```json
  {
    "code": "0",
    "strCode": "success",
    "message": "Lấy danh sách lịch sử import thành công",
    "content": {
      "content": [
        {
          "id": 12,
          "semesterId": 1,
          "semesterCode": "2026-2027-HK1",
          "semesterName": "Học kỳ 1 năm học 2026 - 2027",
          "uploadedBy": 1,
          "fileName": "1606_TKB-hoc-ky-1-nam-hoc-2026-2027.xlsx",
          "importType": "MASTER_TIMETABLE",
          "totalRows": 1500,
          "successRows": 1495,
          "failedRows": 5,
          "status": "PARTIAL",
          "hasErrors": true,
          "createdAt": "2026-11-01T14:30:00",
          "updatedAt": "2026-11-01T14:30:15"
        }
      ],
      "page": 0,
      "size": 20,
      "totalElements": 1,
      "totalPages": 1,
      "last": true
    }
  }
  ```

#### API 2: Xem Chi tiết Lần Import
* **Endpoint:** `GET /api/v1/admin/timetable/import-histories/{id}`
* **Response:** Trả về chi tiết và danh sách lỗi tóm tắt.

#### API 3: Xem Danh sách Lỗi của Lần Import
* **Endpoint:** `GET /api/v1/admin/timetable/import-histories/{id}/errors`
* **Response:**
  ```json
  {
    "code": "0",
    "strCode": "success",
    "message": "Lấy danh sách lỗi import thành công",
    "content": [
      {
        "rowNumber": 45,
        "errorMessage": "Dòng 45: Thiếu mã môn học"
      }
    ]
  }
  ```

#### API 4: Tải lại File Excel Gốc đã Upload
* **Endpoint:** `GET /api/v1/admin/timetable/import-histories/{id}/download`
* **Response:** Binary Stream file Excel (.xlsx), Header `Content-Disposition: attachment; filename="ten_file.xlsx"`.
> **FE Lưu ý:** Tạo thẻ `<a href="..." download>` hoặc dùng `blob` response để người dùng bấm tải lại file gốc đã từng upload.

---

### 3.6. Tra cứu & Giám sát Thời khóa biểu (Monitoring Timetable)

Cán bộ PGV (`ROLE_STAFF`, `ROLE_ADMIN`) có toàn quyền tra cứu TKB để kiểm tra tính chính xác của lịch học và phòng học sau khi import:

1. **Tra cứu TKB Sinh viên:**
   * `GET /api/v1/student/timetable/semesters` (Lấy danh sách kỳ học sinh viên có lịch học).
   * `GET /api/v1/student/timetable/weekly?semesterId={id}&weekNumber={num}` (Xem TKB dạng tuần theo số tuần).
2. **Tra cứu TKB Giảng viên:**
   * `GET /api/v1/lecturer/timetable/semesters`
   * `GET /api/v1/lecturer/timetable/weekly?semesterId={id}&weekNumber={num}`

---

## 4. ĐỊNH DẠNG DỮ LIỆU & QUY CHUẨN RESPONSE CHUNG

Tất cả API tuân thủ cấu trúc đồng nhất:

### Thành công:
```json
{
  "code": "0",
  "strCode": "success",
  "message": "Thông báo thân thiện bằng tiếng Việt",
  "content": { ... }
}
```

### Lỗi nghiệp vụ (Business Exception):
```json
{
  "code": "400",
  "strCode": "validation.invalid_input",
  "message": "Thông báo lỗi chi tiết",
  "apiError": {
    "code": "validation.invalid_input",
    "message": "Thông báo lỗi chi tiết",
    "details": {}
  }
}
```

---

## 5. BẢNG MÃ LỖI FRONTEND CẦN XỬ LÝ (ERROR HANDLING)

| Mã lỗi (`strCode`) | Ý nghĩa nghiệp vụ | Hành động đề xuất cho FE |
| :--- | :--- | :--- |
| `auth.unauthorized` | Chưa đăng nhập hoặc Token hết hạn | Chuyển hướng về trang `/login` |
| `auth.forbidden` | Không đủ quyền (`ROLE_STAFF`/`ROLE_ADMIN`) | Hiển thị màn hình *403 Không có quyền truy cập* |
| `auth.short_code_not_allowed` | Sinh viên/người dùng nhập mã rút gọn (thiếu `DC`) | Thông báo: *"Vui lòng nhập mã đầy đủ (VD: B22DCCN356) hoặc email trường"* |
| `auth.invalid_credentials` | Tên đăng nhập hoặc mật khẩu sai | Hiển thị cảnh báo đỏ trên form đăng nhập |
| `timetable.semester.not-found` | Không tìm thấy học kỳ với ID cung cấp | Thông báo lỗi hoặc tải lại danh sách học kỳ |
| `timetable.semester.code.existed` | Mã học kỳ đã tồn tại | Yêu cầu người dùng đổi mã học kỳ khác |
| `timetable.semester.dates.invalid` | Ngày kết thúc trước hoặc bằng ngày bắt đầu | Yêu cầu chọn ngày kết thúc sau ngày bắt đầu |
| `timetable.class_section.not_found` | Không tìm thấy lớp học phần để import DSSV | Kiểm tra lại `sectionId` |
| `timetable.file.empty` | File upload rỗng (0 bytes) | Chặn upload và thông báo người dùng chọn lại file |
| `timetable.file.invalid-format` | File tải lên không đúng định dạng Excel | Yêu cầu chỉ tải file `.xlsx` hoặc `.xls` |
| `timetable.file.too-large` | Dung lượng file vượt quá 50MB | Thông báo file vượt quá dung lượng cho phép |
