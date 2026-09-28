# Quy ước response và xử lý lỗi

## 1. Response thành công

Mọi API trả về wrapper `BaseResponse<T>` với cấu trúc:

```json
{
  "code": "0",
  "strCode": "success",
  "message": "SUCCESSFUL",
  "content": {}
}
```

Cách dùng:

```java
return BaseResponse.success(response);
return BaseResponse.created(response);
return BaseResponse.success("Created successfully");
```

Lưu ý: overload `success(String)` coi chuỗi là `message`, không phải `content`. Không trả raw
`String` làm dữ liệu API; hãy tạo response DTO có tên field rõ ràng.

Với phân trang, ưu tiên `PageResponseDTO<T>` khi client cần metadata trang. `BaseResponse.ofPage`
chỉ trả danh sách content theo contract hiện tại và không chứa metadata.

## 2. Ba thành phần của một lỗi nghiệp vụ

Hệ thống dùng đúng ba thành phần:

1. `StatusCode`: loại kết quả và HTTP status, ví dụ `NOT_EXISTED` hoặc `INVALID_INPUT_DATA`.
2. `strCode`: message key ổn định, ví dụ `error.meeting.not-found`.
3. `args`: tham số dùng để format message, ví dụ ID hoặc tên đối tượng.

Không tạo `ErrorCode`, `CommonErrorCode` hoặc một enum lỗi riêng song song với cơ chế này.

## 3. Vị trí error constant

Lỗi dùng chung:

```text
org.ptit.meeting.common.constant.ErrorConstants
```

Mỗi module bắt buộc có một class error constant riêng:

```java
package org.ptit.meeting.modules.room.constant;

public final class RoomErrorConstants {

  public static final String NOT_FOUND = "error.room.not-found";
  public static final String CODE_EXISTED = "error.room.code-existed";

  private RoomErrorConstants() {
  }
}
```

Message tương ứng đặt trong `src/main/resources/messages.properties`:

```properties
error.room.not-found=Phòng họp không tồn tại
error.room.code-existed=Mã phòng họp đã tồn tại
```

Khi bổ sung ngôn ngữ khác, tạo `messages_<locale>.properties` với cùng tập key.

Không đặt key của module vào `common.constant.ErrorConstants`. `ErrorConstants` chỉ dành cho
lỗi dùng chung toàn hệ thống; key nào thuộc nghiệp vụ phải nằm trong module sở hữu.

## 4. Ném lỗi từ service

Sử dụng factory method phù hợp của `BusinessException`:

```java
Room room = roomRepository.findById(id)
    .orElseThrow(() -> BusinessException.notExisted(RoomErrorConstants.NOT_FOUND, id));

if (roomRepository.existsByCode(request.code())) {
  throw BusinessException.existed(RoomErrorConstants.CODE_EXISTED, request.code());
}
```

Mapping hiện tại:

| Factory | StatusCode | HTTP status |
|---|---|---|
| `invalidInput` | `INVALID_INPUT_DATA` | 400 |
| `invalidInputFile` | `INVALID_INPUT_FILE` | 400 |
| `existed` | `EXISTED` | 400 |
| `notExisted` | `NOT_EXISTED` | 404 |
| `unauthorized` | `AUTHORIZATION_FAILED` | 403 |

Nếu không có factory phù hợp, có thể gọi constructor với `StatusCode` hiện có. Chỉ thêm
`StatusCode` mới khi đó là một loại kết quả toàn hệ thống, không phải một lỗi riêng feature.

## 5. GlobalExceptionHandler

`GlobalExceptionHandler` chịu trách nhiệm:

- Bắt `BusinessException`.
- Dùng `MessageUtil` tra `strCode` trong message bundle.
- Chọn HTTP status từ `StatusCode`.
- Đóng gói lỗi bằng `BaseResponse.error(...)`.
- Chuyển lỗi validation và dữ liệu sai định dạng thành response thống nhất.
- Che thông tin lỗi nội bộ khi gặp exception chưa dự kiến.

Controller và service không tự lặp lại logic dựng response lỗi.

## 6. Validation

Request DTO dùng Jakarta Validation:

```java
public record RoomCreateRequest(
    @NotBlank(message = "{validation.room.name.required}") String name,
    @Positive(message = "{validation.room.capacity.positive}") Integer capacity
) {
}
```

Message key phải có trong `messages.properties`. Validation cần database hoặc phụ thuộc trạng
thái hiện tại không đặt trong annotation; kiểm tra tại service và ném `BusinessException`.

## 7. Điều không được làm

- Không throw `RuntimeException` cho lỗi nghiệp vụ dự kiến.
- Không hardcode thông báo tiếng Việt trong service/controller.
- Không trả stack trace, SQL hoặc thông tin nội bộ cho client.
- Không tạo response lỗi riêng cho từng controller.
- Không dùng HTTP 200 cho lỗi nghiệp vụ nếu `StatusCode` đã định nghĩa status phù hợp.
