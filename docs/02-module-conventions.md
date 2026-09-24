# Convention cho module nghiệp vụ

## 1. Cấu trúc chuẩn

Mỗi feature đặt tại `org.ptit.meeting.modules.<feature>`:

```text
modules/<feature>
|-- constant
|-- controller
|-- dto
|   |-- request
|   `-- response
|-- entity
|-- mapper
|-- repository
`-- service
    `-- impl
```

`constant` là package chuẩn bắt buộc của mỗi module. Mỗi module tạo class
`<Feature>ErrorConstants` để tập trung message key lỗi do module sở hữu. Khi chưa có nhiều lỗi,
class bắt đầu bằng các key thực tế tối thiểu như `NOT_FOUND`; không tạo key dự phòng không có
use case. Các package mở rộng như `event`, `scheduler`, `listener`, `exporter` hoặc `importer`
chỉ tạo khi feature thực sự cần.

## 2. Quy tắc đặt tên

Giả sử feature là `room` và resource chính là `Room`:

| Vai trò | Package | Tên chuẩn |
|---|---|---|
| REST controller | `room.controller` | `RoomController` |
| Service interface | `room.service` | `RoomService` |
| Service implementation | `room.service.impl` | `RoomServiceImpl` |
| JPA repository | `room.repository` | `RoomRepository` |
| Query constant | `room.repository` | `RoomQueries` |
| Entity | `room.entity` | `Room` |
| Mapper | `room.mapper` | `RoomMapper` |
| Create request | `room.dto.request` | `RoomCreateRequest` |
| Update request | `room.dto.request` | `RoomUpdateRequest` |
| Search request | `room.dto.request` | `RoomSearchRequest` |
| Response | `room.dto.response` | `RoomResponse` |
| Error constants | `room.constant` | `RoomErrorConstants` |

Quy định:

- Package viết thường, tên feature dùng một từ hoặc từ ghép viết liền rõ nghĩa.
- Class dùng PascalCase; field và method dùng camelCase; constant dùng UPPER_SNAKE_CASE.
- Không dùng tên chung chung như `Manager`, `Processor`, `Helper` nếu vai trò có thể diễn đạt cụ thể.
- API input kết thúc bằng `Request`; API output kết thúc bằng `Response`.
- Chỉ dùng suffix `DTO` cho model dùng chung không thuộc riêng input hoặc output.
- Interface service không có prefix `I`.
- Implementation luôn kết thúc bằng `ServiceImpl` và nằm trong `service.impl`.
- Error constant class luôn kết thúc bằng `ErrorConstants`, là class `final` và có constructor private.

## 3. Dependency nội bộ module

```text
controller
    -> dto.request / dto.response
    -> service

service.impl
    -> service
    -> repository
    -> mapper
    -> entity
    -> dto
    -> constant

repository -> entity
mapper     -> entity + dto
```

Không được:

- Inject repository vào controller.
- Trả entity trực tiếp từ controller.
- Đặt validation nghiệp vụ trong mapper.
- Đặt câu query hoặc thao tác `EntityManager` trong service.
- Inject controller vào bất kỳ layer nào.
- Truy cập trực tiếp repository của module khác.

## 4. Khi module có nhiều resource

Một module có thể chứa nhiều resource nếu chúng thuộc cùng một feature và vòng đời nghiệp vụ.
Không tạo thêm một tầng package theo layer toàn cục. Ví dụ hợp lệ:

```text
modules/meeting/entity/Meeting.java
modules/meeting/entity/MeetingParticipant.java
modules/meeting/repository/MeetingRepository.java
modules/meeting/repository/MeetingParticipantRepository.java
```

Nếu một nhóm resource có nghiệp vụ độc lập, API và vòng đời riêng, ưu tiên tách thành module mới.

## 5. Quan hệ giữa các module

- Gọi qua service interface của module sở hữu dữ liệu.
- Không dùng repository của module khác để đi tắt business rule.
- Tránh dependency vòng tròn. Nếu A cần B và B cần A, xem lại ranh giới module hoặc tách một
  capability dùng chung có trách nhiệm rõ ràng.
- Event chỉ dùng khi cần giảm coupling hoặc xử lý sau transaction; không dùng event để che
  một dependency vòng tròn chưa được thiết kế rõ.
