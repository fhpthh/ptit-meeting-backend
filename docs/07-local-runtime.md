# Chạy local và tự tạo bảng

## 1. Quyết định hiện tại

Trong giai đoạn chạy local của MVP, dự án vẫn giữ Liquibase trong dependency nhưng tắt mặc định. Hibernate
là nguồn tạo/cập nhật bảng từ JPA entity với cấu hình:

```yaml
spring.liquibase.enabled: ${LIQUIBASE_ENABLED:false}
spring.jpa.hibernate.ddl-auto: ${JPA_DDL_AUTO:update}
```

Không xóa Liquibase khỏi dự án. Đây chỉ là cấu hình bỏ qua migration khi phát triển local. Hibernate chỉ tạo
bảng bên trong schema đã tồn tại, không tạo database/schema MySQL. Khi chuyển sang migration có kiểm soát,
đặt `LIQUIBASE_ENABLED=true`, bổ sung changelog phù hợp và đổi `JPA_DDL_AUTO=validate`.

## 2. Thành phần cần có

- Java 21 và Maven.
- MySQL đang chạy, có schema `ptit_meeting` hoặc schema được khai báo trong `DB_URL`.
- Redis khi kiểm tra login, refresh token hoặc logout. Redis không cần để mở Swagger hay khởi động ứng dụng,
  nhưng auth runtime hiện dùng Redis để rate limit và lưu refresh token.
- LiveKit chỉ cần khi kiểm tra endpoint join thật. Có thể giữ `LIVEKIT_ENABLED=false` để kiểm tra CRUD room và
  vòng đời session.
- MinIO/Silo cần khi kiểm tra ghi hình. Trình duyệt của giảng viên ghi màn hình và tải trực tiếp từng
  multipart lên bucket; backend chỉ cấp presigned URL và không nhận byte video.

Không bắt buộc Docker. MySQL/Redis có thể chạy trực tiếp trên máy; LiveKit có thể dùng LiveKit Cloud.

## 3. Biến môi trường

Sao chép `.env.example` thành `.env`, sau đó điền thông tin local. `application.yml` tự nạp file `.env` ở
thư mục gốc nếu file tồn tại. `.env` đã bị loại khỏi Git để không commit secret.

Các biến tối thiểu để khởi động với MySQL:

```properties
DB_URL=jdbc:mysql://localhost:3306/ptit_meeting?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=your-password
JPA_DDL_AUTO=update
LIQUIBASE_ENABLED=false
JWT_SECRET_KEY=replace-with-a-random-secret-at-least-32-bytes
```

Nếu schema chưa có, tạo một lần:

```sql
CREATE DATABASE ptit_meeting
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

## 4. Khởi động hạ tầng Meeting bằng Docker Compose

MySQL tiếp tục chạy trực tiếp trên máy. `compose.yml` quản lý Redis, LiveKit và MinIO/Silo.

Đảm bảo `.env` có cùng API key/secret mà backend và LiveKit sẽ sử dụng:

```properties
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

LIVEKIT_ENABLED=true
LIVEKIT_API_URL=http://localhost:7880
LIVEKIT_WS_URL=ws://localhost:7880
LIVEKIT_API_KEY=localdevkey
LIVEKIT_API_SECRET=replace-with-a-random-local-livekit-secret
LIVEKIT_JOIN_TOKEN_TTL_MS=600000
LIVEKIT_WEBHOOK_URL=http://host.docker.internal:8080/api/v1/integrations/livekit/webhook

RECORDING_ENABLED=true
RECORDING_CLEANUP_INTERVAL_MS=120000
RECORDING_STALE_UPLOAD_TIMEOUT_MS=900000
RECORDING_UPLOAD_PART_URL_TTL_MS=900000
RECORDING_STORAGE_BUCKET=meeting-recordings
RECORDING_STORAGE_REGION=us-east-1
RECORDING_STORAGE_PUBLIC_BASE_URL=http://localhost:9000
RECORDING_STORAGE_ENDPOINT=http://localhost:9000
RECORDING_STORAGE_ACCESS_KEY=ptitrecording
RECORDING_STORAGE_SECRET_KEY=replace-with-a-random-local-storage-secret
RECORDING_STORAGE_FORCE_PATH_STYLE=true
RECORDING_STORAGE_CORS_ALLOWED_ORIGIN=http://localhost:3000
```

`LIVEKIT_WEBHOOK_URL` là URL mà container LiveKit dùng để gọi ngược về backend chạy trên máy host.
Với Docker Desktop, giữ `host.docker.internal`; nếu backend chạy ở máy khác thì thay bằng địa chỉ mà
container LiveKit có thể truy cập. Backend phải đang chạy để nhận webhook kết nối và attendance. URL
recording là public-read và không hết hạn; bất kỳ ai có URL đều có thể đọc object tương ứng.

Khởi động hạ tầng:

```powershell
docker compose up -d
docker compose ps
```

Theo dõi log nếu cần:

```powershell
docker compose logs -f redis livekit minio
```

Các endpoint/port local:

- Redis: `localhost:6379`.
- LiveKit API và WebSocket: `http://localhost:7880` / `ws://localhost:7880`.
- LiveKit WebRTC TCP: `localhost:7881`.
- LiveKit WebRTC UDP mux: `localhost:7882/udp`.
- LiveKit webhook receiver: `POST /api/v1/integrations/livekit/webhook` trên backend.
- MinIO API/public recording: `http://localhost:9000`.
- MinIO Console: `http://localhost:9001`.

Compose chỉ bind vào `127.0.0.1`, phù hợp frontend và backend chạy trên cùng máy. Muốn thiết bị khác trong LAN
kết nối thì phải thay bind address và `rtc.node_ip` bằng IP LAN của máy, đồng thời mở Windows Firewall.

Dừng container nhưng giữ dữ liệu Redis và recording:

```powershell
docker compose down
```

Không dùng `docker compose down -v` nếu muốn giữ dữ liệu Redis và recording vì tùy chọn `-v` xóa
cả `redis_data` và `minio_data`.

## 5. Khởi động backend

```powershell
mvn spring-boot:run
```

Lần khởi động đầu tiên với `JPA_DDL_AUTO=update`, Hibernate tạo các bảng và index được khai báo trong entity.
Các lần sau Hibernate đối chiếu schema và áp dụng thay đổi có thể suy ra. Chế độ này không chủ động tạo lại toàn
bộ schema như `create`/`create-drop`, nhưng vẫn chỉ nên dùng cho local vì thay đổi cột phức tạp có thể có rủi ro.

## 6. Swagger

Sau khi ứng dụng chạy:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Để gọi API meeting:

1. Gọi `POST /api/v1/auth/login` để lấy access token. Database trống cần có user hợp lệ trước hoặc phải cấu
   hình đăng nhập Microsoft.
2. Nhấn **Authorize** trên Swagger.
3. Dán access token. Swagger tự gửi header `Authorization: Bearer <token>`.

## 7. Giới hạn của Hibernate update

`ddl-auto=update` phù hợp thử nghiệm local nhưng không thay thế migration production: nó không quản lý lịch sử
schema, rollback hoặc các chuyển đổi dữ liệu phức tạp. Không dùng `create` hoặc `create-drop` với database có
dữ liệu cần giữ vì các chế độ đó có thể làm mất bảng/dữ liệu.

Nếu `meeting_recordings` đã được tạo theo contract Egress cũ, chạy một lần
`src/main/resources/db/sql/migrate_meeting_recording_browser_upload.sql`. Database mới không cần chạy file
này vì Hibernate tạo bảng trực tiếp từ entity hiện tại.
