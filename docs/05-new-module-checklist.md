# Checklist tạo module mới

Tài liệu này là quy trình bắt buộc cho cả lập trình viên và AI.

## 1. Trước khi code

- [ ] Đọc toàn bộ tài liệu được liệt kê trong `docs/README.md`.
- [ ] Xác định rõ feature sở hữu nghiệp vụ và tên package viết thường.
- [ ] Kiểm tra module hiện có để tránh tạo trùng resource hoặc capability.
- [ ] Xác định schema, ID type, field audit và quan hệ dữ liệu thực tế.
- [ ] Xác định API request/response; không dùng entity làm contract.
- [ ] Tìm shared class hiện có trước khi tạo abstraction mới.

## 2. Tạo cấu trúc

Ví dụ module `room`:

```text
src/main/java/org/ptit/meeting/modules/room
|-- constant
|   `-- RoomErrorConstants.java
|-- controller
|   `-- RoomController.java
|-- dto
|   |-- request
|   |   |-- RoomCreateRequest.java
|   |   `-- RoomUpdateRequest.java
|   `-- response
|       `-- RoomResponse.java
|-- entity
|   `-- Room.java
|-- mapper
|   `-- RoomMapper.java
|-- repository
|   `-- RoomRepository.java
`-- service
    |-- impl
    |   `-- RoomServiceImpl.java
    `-- RoomService.java
```

Các package chuẩn trong cây trên phải được tuân thủ. `constant/<Feature>ErrorConstants.java`
là bắt buộc và chứa ít nhất các message key thực tế của module. Package mở rộng ngoài cây chuẩn
chỉ tạo khi use case yêu cầu; không tạo class rỗng để “dự phòng”.

## 3. Thứ tự triển khai khuyến nghị

1. Entity theo schema thực tế.
2. Repository và query cần thiết.
3. Request/response DTO cùng validation hình thức.
4. Error constants và message bundle.
5. Mapper.
6. Service interface.
7. Service implementation và transaction boundary.
8. Controller.
9. Unit/integration/web tests tương ứng.

## 4. Checklist code

### Entity và repository

- [ ] Entity nằm trong `modules.<feature>.entity`.
- [ ] ID và column mapping đúng schema.
- [ ] Chỉ kế thừa `BaseEntity` khi schema tương thích.
- [ ] Repository nằm trong module sở hữu entity.
- [ ] Service không chứa JPQL/query string.

### DTO và mapper

- [ ] Request và response tách riêng.
- [ ] Request có Jakarta Validation phù hợp.
- [ ] Không expose entity hoặc field nội bộ.
- [ ] Mapper không gọi repository/service.
- [ ] Không tạo một mapping framework riêng cho module.

### Service

- [ ] Có interface trong `service` và implementation trong `service.impl`.
- [ ] Business logic nằm trong service.
- [ ] Dependency dùng constructor injection.
- [ ] Write operation có transaction boundary phù hợp.
- [ ] Không truy cập trực tiếp repository module khác.

### Controller

- [ ] Chỉ xử lý HTTP và gọi service.
- [ ] Dùng request/response DTO.
- [ ] Trả `BaseResponse<T>`.
- [ ] Không có business rule hoặc thao tác repository.
- [ ] Không tự bắt exception đã được `GlobalExceptionHandler` xử lý.

### Error handling

- [ ] Module có package `constant` và class `<Feature>ErrorConstants`.
- [ ] Error key riêng module nằm trong `*ErrorConstants`.
- [ ] Message key đã được thêm vào `messages.properties`.
- [ ] Dùng `BusinessException` và `StatusCode` hiện có.
- [ ] Không tạo `ErrorCode`, `CommonErrorCode` hoặc wrapper lỗi mới.

### Test và rà soát

- [ ] Có test cho business rule chính và các nhánh lỗi.
- [ ] Chạy `mvn test` thành công.
- [ ] Không có import/package `layer`, `domain`, `application`, `infrastructure` toàn cục.
- [ ] Không có secret, endpoint hoặc credential hardcode.
- [ ] Cập nhật docs nếu có convention mới được thống nhất.

## 5. Lệnh kiểm tra nhanh

```powershell
rg "org\.ptit\.meeting\.(layer|domain|application|infrastructure)" src
rg "CommonErrorCode|interface ErrorCode|enum ErrorCode" src
mvn test
```

Hai lệnh `rg` phải không trả về kết quả, trừ khi từ khóa xuất hiện trong tài liệu hoặc test
kiểm tra convention.

## 6. Definition of Done

Một module chỉ được coi là hoàn tất khi:

- Đúng cấu trúc package và naming convention.
- API không làm lộ entity.
- Business rule nằm trong service và có test.
- Error/response tuân theo base chung.
- Build và test thành công.
- Không tạo convention thứ hai cạnh convention hiện tại.
