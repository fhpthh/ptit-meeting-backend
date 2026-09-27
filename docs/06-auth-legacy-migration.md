# Ngoại lệ migration cho module auth

Module `org.ptit.meeting.modules.auth` được chuyển từ source tham chiếu
`D:\refactor-login\ptit-meeting-backend` theo yêu cầu giữ nguyên luồng nghiệp vụ hiện tại.

## Phạm vi phải giữ nguyên

Các package sau giữ nguyên tên class, tên method và nội dung điều phối nghiệp vụ; chỉ được thay
đổi package/import để nằm trong module mới:

- `auth.controller`
- `auth.facade` và `auth.facade.impl`
- `auth.service` và `auth.service.impl`
- `auth.repository`
- `auth.entity`

`AuthController -> AuthFacade -> các service` là luồng có chủ đích của module này. Không gộp
`AuthFacade` vào `AuthService` nếu chưa có yêu cầu thay đổi nghiệp vụ riêng.

Riêng chữ ký HTTP của controller được phép thay wrapper response dùng chung, nhưng không được đổi
endpoint, request, lời gọi facade hoặc các nhánh xử lý nghiệp vụ.

## Phần áp dụng convention chung

Module auth không sở hữu bộ exception hoặc response riêng. Toàn bộ lỗi auth dùng chung:

- `exception.BusinessException` để biểu diễn lỗi nghiệp vụ.
- `common.constant.StatusCode` để xác định loại kết quả và HTTP status.
- `modules.auth.constant.AuthErrorConstants` để sở hữu message key của module.
- `exception.GlobalExceptionHandler` để dịch lỗi và đóng gói response.
- `common.wrapper.BaseResponse` và `common.wrapper.ApiError` cho HTTP response.

`MessageService` và `MessageKeyConstant` được giữ lại cho thông báo thành công của controller vì
đây là một phần chữ ký/luồng hiện tại từ source tham chiếu. Các key lỗi không được đặt trong
`MessageKeyConstant` mà phải đặt trong `AuthErrorConstants`.

Thông báo thành công, lỗi và Jakarta Validation của auth đều được tra từ bundle dùng chung
`src/main/resources/i18n/messages*.properties`. DTO auth được phép thay câu chữ validation
hardcode thành message key vì thay đổi này không tác động điều kiện hoặc luồng nghiệp vụ.

Việc đổi cơ chế exception/response chỉ là chuyển đổi kỹ thuật theo convention của `src`. Không
được làm thay đổi endpoint, thứ tự gọi facade/service, điều kiện rẽ nhánh, thao tác repository hay
trạng thái entity của luồng auth hiện tại.
