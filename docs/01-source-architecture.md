# Cấu trúc source và ranh giới kiến trúc

## 1. Package gốc

Toàn bộ Java source của ứng dụng nằm dưới:

```text
org.ptit.meeting
```

Không tạo package Java nằm ngoài package gốc này nếu chưa có quyết định thay đổi kiến trúc.

## 2. Cấu trúc top-level

```text
org.ptit.meeting
|-- common
|   |-- base
|   |-- constant
|   |-- dto
|   |   |-- request
|   |   `-- response
|   `-- wrapper
|-- config
|-- exception
|-- integration
|-- modules
|   `-- <feature>
`-- utils
```

### `common`

Chứa thành phần dùng chung thực sự cho nhiều module:

- `base`: abstraction nền tảng như `BaseEntity`, `BaseMapper`.
- `constant`: mã trạng thái và constant toàn hệ thống.
- `dto`: request/response dùng chung như phân trang.
- `wrapper`: định dạng response chung.

Không đặt business rule, repository hoặc service của một feature vào `common`.
Một class chỉ được chuyển vào `common` khi có nhu cầu tái sử dụng rõ ràng từ nhiều module.

### `config`

Chứa cấu hình Spring dùng toàn ứng dụng: JPA, Jackson, web, security, async hoặc OpenAPI.
Không đặt workflow nghiệp vụ hoặc truy vấn dữ liệu trong package này.

### `exception`

Chứa exception và handler dùng toàn hệ thống. Lỗi riêng module được biểu diễn bằng message
key trong `modules.<feature>.constant`, không tạo một hệ thống exception song song.

### `integration`

Chứa adapter kỹ thuật cho hệ thống ngoài như mail, Redis, object storage hoặc API bên thứ ba.
SDK bên ngoài phải được bọc sau client/service kỹ thuật trong package này. Không đặt quyết
định nghiệp vụ vào integration.

### `modules`

Chứa toàn bộ nghiệp vụ, chia theo feature. Ví dụ:

```text
modules.meeting
modules.room
modules.user
modules.notification
```

Mỗi module tự sở hữu controller, service, repository, entity, mapper, DTO và constant của nó.

### `utils`

Chứa helper stateless, tổng quát và không phụ thuộc nghiệp vụ. Utility không được gọi trực
tiếp repository hoặc service của module.

## 3. Hướng dependency

Luồng chuẩn trong một request:

```text
Controller -> Service interface -> ServiceImpl -> Repository -> Database
                    |                  |
                    `------ DTO/Mapper-'
```

Quy tắc:

- Controller chỉ phụ thuộc service và DTO.
- Controller không gọi repository trực tiếp.
- Service chứa business logic và điều phối transaction.
- Repository chỉ truy cập dữ liệu.
- Mapper chỉ chuyển đổi dữ liệu, không quyết định nghiệp vụ.
- Module A không truy cập repository của module B. Nếu cần, gọi service công khai của module B.
- `common`, `config`, `exception`, `integration`, `utils` không phụ thuộc ngược vào service
  hoặc repository của một module cụ thể, trừ adapter được thiết kế rõ ràng và có tài liệu.

## 4. Package bị cấm

Không tạo lại các package kiến trúc hexagonal toàn cục:

```text
org.ptit.meeting.layer
org.ptit.meeting.domain
org.ptit.meeting.application
org.ptit.meeting.infrastructure
```

Không tạo package top-level mới nếu chức năng có thể đặt đúng vào sáu package hiện tại.

## 5. Nguyên tắc thay đổi kiến trúc

- Ưu tiên giải pháp đơn giản nhất phù hợp convention đang có.
- Không tạo abstraction chỉ vì dự đoán có thể dùng trong tương lai.
- Trước khi tạo base class/interface mới, phải kiểm tra class hiện có và có ít nhất một use case rõ ràng.
- Khi thay đổi ranh giới package hoặc dependency, cập nhật tài liệu trong cùng pull request.
