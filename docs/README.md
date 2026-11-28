# Hướng dẫn kiến trúc PTIT Meeting Backend

Thư mục này là nguồn quy ước chính thức cho source code của dự án. Lập trình viên và AI
phải đọc tài liệu trước khi tạo module mới hoặc thay đổi kiến trúc trong `src`.

## Local runtime

- [07-local-runtime.md](./07-local-runtime.md): cấu hình MySQL tự tạo bảng bằng Hibernate,
  các dependency runtime và Swagger UI cho giai đoạn MVP.

## Required reading

Đọc theo thứ tự:

1. [01-source-architecture.md](./01-source-architecture.md): cấu trúc tổng thể và ranh giới package.
2. [02-module-conventions.md](./02-module-conventions.md): cấu trúc và cách đặt tên trong module.
3. [03-implementation-rules.md](./03-implementation-rules.md): quy tắc triển khai từng layer.
4. [04-error-response-conventions.md](./04-error-response-conventions.md): response, exception và message.
5. [05-new-module-checklist.md](./05-new-module-checklist.md): quy trình và checklist thêm module.

## Ngoại lệ có chủ đích

- [06-auth-legacy-migration.md](./06-auth-legacy-migration.md): ngoại lệ tạm thời của module `auth`
  nhằm giữ nguyên luồng đăng nhập đã có trong source tham chiếu.

## Tài liệu đặc tả giải pháp và nghiệp vụ

- [semester-management-solution-design.md](./semester-management-solution-design.md): Giải pháp quản lý học kỳ và năm học.
- [timetable-solution-design.md](./timetable-solution-design.md): Giải pháp import thời khóa biểu tổng (Master Timetable).
- [pgv-features-specification.md](./pgv-features-specification.md): Đặc tả toàn bộ tính năng, luồng giao diện và hợp đồng API dành cho vai trò Phòng Giáo vụ (`ROLE_STAFF`, `ROLE_ADMIN`).
- [notification-architecture-and-flow.md](./notification-architecture-and-flow.md): Phân tích kiến trúc, thiết kế DB, sơ đồ luồng (Flowchart/Sequence), Input/Output và Q&A kỹ thuật cho module Notification (Phase 1 – Phase 3).

## Quy trình bắt buộc dành cho AI

Trước khi sửa source, AI phải:

1. Đọc toàn bộ các tài liệu trong phần Required reading.
2. Kiểm tra cấu trúc và class dùng chung đang tồn tại trong `src/main/java/org/ptit/meeting`.
3. Xác định module sở hữu nghiệp vụ; không đặt code nghiệp vụ vào `common` hoặc `utils`.
4. Tái sử dụng convention hiện có; không tự tạo thêm base class, wrapper hoặc hệ thống lỗi mới.
5. Sau khi triển khai, chạy test và đối chiếu checklist trong `05-new-module-checklist.md`.

Nếu yêu cầu mới làm thay đổi convention, phải cập nhật tài liệu này cùng source code.
Không duy trì hai convention song song.

## Nguyên tắc ngắn gọn

- Kiến trúc: package-by-feature kết hợp 3-layer nhẹ.
- Package nghiệp vụ: `modules.<feature>`.
- Mỗi module có package `constant` và class `<Feature>ErrorConstants` cho message key riêng.
- Luồng gọi: `Controller -> Service -> Repository`.
- API không nhận hoặc trả trực tiếp JPA Entity.
- Lỗi nghiệp vụ: `BusinessException + StatusCode + strCode`.
- Response API: `BaseResponse<T>`.
- Message và validation: chỉ dùng bundle `src/main/resources/i18n/messages*.properties`.
- Không khôi phục cấu trúc hexagonal `layer/domain/application/infrastructure`.

## Tìm kiếm giảng viên - 2026-11-28

`GET /api/v1/admin/lecturers?keyword=...` tìm theo một phần họ tên, mã, email hoặc
khoa/bộ môn, không phân biệt chữ hoa/thường. Các điều kiện keyword được nhóm OR,
kết hợp AND với loại tài khoản LECTURER, trạng thái và bộ lọc department hiện có.
Frontend Admin/Staff dùng một ô keyword chung thay cho ô nhập riêng khoa/bộ môn.
Theo yêu cầu người dùng, không tạo thêm file test; chạy lại bộ test hiện có bằng `mvn test`.
