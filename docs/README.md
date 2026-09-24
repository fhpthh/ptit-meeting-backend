# Hướng dẫn kiến trúc PTIT Meeting Backend

Thư mục này là nguồn quy ước chính thức cho source code của dự án. Lập trình viên và AI
phải đọc tài liệu trước khi tạo module mới hoặc thay đổi kiến trúc trong `src`.

## Required reading

Đọc theo thứ tự:

1. [01-source-architecture.md](./01-source-architecture.md): cấu trúc tổng thể và ranh giới package.
2. [02-module-conventions.md](./02-module-conventions.md): cấu trúc và cách đặt tên trong module.
3. [03-implementation-rules.md](./03-implementation-rules.md): quy tắc triển khai từng layer.
4. [04-error-response-conventions.md](./04-error-response-conventions.md): response, exception và message.
5. [05-new-module-checklist.md](./05-new-module-checklist.md): quy trình và checklist thêm module.

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
- Không khôi phục cấu trúc hexagonal `layer/domain/application/infrastructure`.
