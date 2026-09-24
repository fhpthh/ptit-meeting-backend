# Quy tắc triển khai code

## 1. Controller

Controller chịu trách nhiệm HTTP, không chứa business logic.

- Dùng `@RestController` và một `@RequestMapping` ở cấp class.
- Nhận request bằng DTO và dùng `@Valid` khi DTO có Jakarta Validation.
- Gọi service interface, không gọi `ServiceImpl` hoặc repository trực tiếp.
- Trả `BaseResponse<T>`; không trả entity.
- Chỉ xử lý HTTP status/header/path parameter. Exception để `GlobalExceptionHandler` xử lý.
- Không dùng `try/catch` chỉ để đổi mọi lỗi thành HTTP 500.

Ví dụ:

```java
@RestController
@RequestMapping("/api/v1/rooms")
public class RoomController {

  private final RoomService roomService;

  public RoomController(RoomService roomService) {
    this.roomService = roomService;
  }

  @PostMapping
  public BaseResponse<RoomResponse> create(@Valid @RequestBody RoomCreateRequest request) {
    return BaseResponse.created(roomService.create(request));
  }
}
```

## 2. Request và Response DTO

- Request chỉ chứa dữ liệu client được phép gửi.
- Response chỉ chứa dữ liệu API công khai.
- Không dùng entity làm request/response.
- Validation hình thức (`@NotNull`, `@NotBlank`, `@Size`,...) đặt trên request.
- Validation nghiệp vụ cần database hoặc trạng thái hệ thống đặt trong service.
- Không đặt repository/service vào DTO.
- Không tái sử dụng cùng một request cho create và update nếu quy tắc validation khác nhau.

## 3. Service

- Service interface định nghĩa use case công khai của module.
- `ServiceImpl` chứa business rule và điều phối repository/mapper/integration.
- Constructor injection là mặc định; không dùng field injection.
- Method ghi dữ liệu dùng `@Transactional` tại service implementation.
- Method chỉ đọc có thể dùng `@Transactional(readOnly = true)`.
- Không để controller điều phối nhiều repository thay cho service.
- Lỗi nghiệp vụ phải dùng `BusinessException`, theo tài liệu xử lý lỗi.

Ví dụ interface:

```java
public interface RoomService {
  RoomResponse create(RoomCreateRequest request);
  RoomResponse getById(String id);
}
```

## 4. Repository

- CRUD và query đơn giản: kế thừa `JpaRepository<Entity, IdType>`.
- Query đơn giản: dùng derived method như `findByCode`, `existsByNameIgnoreCase`.
- JPQL cố định và dài: đặt chuỗi query trong class final `XxxQueries`, dùng với `@Query`.
- Query động phức tạp: tạo custom repository implementation trong module, không ghép query
  string trong service.
- Repository không ném `BusinessException` và không chứa business decision.
- Không annotate interface kế thừa `JpaRepository` bằng `@Component` hoặc `@Service`.

## 5. Entity

- Mapping phải theo schema database thực tế; khai báo rõ `@Table` và `@Column` khi cần.
- Entity có ID chuỗi UUID và `createdAt/updatedAt` chuẩn có thể kế thừa `BaseEntity`.
- Không bắt buộc kế thừa `BaseEntity` nếu schema không tương thích.
- Tránh `@Data` trên entity vì có thể tạo `equals/hashCode/toString` không an toàn với JPA.
- Không serialize entity trực tiếp ra API.
- Quan hệ JPA chỉ khai báo khi thực sự cần; cân nhắc lưu FK ID để tránh fetch graph ngoài ý muốn.

## 6. Mapper

- Mapper chỉ chuyển đổi Entity <-> DTO.
- Mapper CRUD đơn giản có thể triển khai `BaseMapper<E, D>`.
- Không query database hoặc gọi external service trong mapper.
- Mapping cần business context phải được điều phối ở service.
- Nếu dự án bổ sung MapStruct, khai báo dependency và annotation processor tập trung trong
  `pom.xml`; không tự thêm một công nghệ mapper riêng cho từng module.

## 7. Constant và enum

- Constant toàn hệ thống đặt tại `common.constant`.
- Constant riêng feature đặt tại `modules.<feature>.constant`.
- Mỗi module phải có `<Feature>ErrorConstants`; chỉ khai báo các message key do module đó sở hữu.
- Class constant là `final`, có constructor private và không chứa state thay đổi.
- Enum dùng chung mới đặt trong `common`; enum riêng nghiệp vụ nằm trong module sở hữu.
- Không tạo enum `ErrorCode` hoặc `CommonErrorCode`. Error key là string constant; loại response
  dùng `StatusCode` hiện có.

## 8. Integration và utility

- Không gọi trực tiếp SDK bên ngoài từ controller.
- Secret, endpoint và credential phải lấy từ configuration/environment, không hardcode.
- Utility phải stateless và không chứa nghiệp vụ của một feature.
- Nếu helper chỉ phục vụ một module, đặt nó trong module thay vì `utils`.

## 9. Chất lượng và test

- Mỗi business rule quan trọng phải có unit test ở service.
- Custom query cần integration test khi có thể.
- Controller có logic binding/validation đáng kể cần web test.
- Tên test mô tả hành vi và kết quả, không đặt `test1`, `testCreate` chung chung.
- Luôn chạy:

```powershell
mvn test
```

Không coi task hoàn tất nếu source không compile hoặc test liên quan đang thất bại.
