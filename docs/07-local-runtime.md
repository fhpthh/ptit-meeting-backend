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

## 8. LiveKit media subscriptions và lượt trình chiếu

Frontend kết nối với `autoSubscribe: false`, chỉ nhận các camera có ô đang hiển thị.
Camera dùng simulcast, capture tối đa 720p/15fps; micro dùng Opus speech 24 Kbps với
DTX/RED. Các micro được phép publish vẫn được nhận độc lập với số ô camera; thay đổi
này không thêm workflow duyệt nhóm micro hoặc tự cắt audio theo người đang hiển thị.

Token join không cấp quyền publish `screen_share`/`screen_share_audio` mặc định.
`canShareScreen` trong join response và participant metadata biểu diễn quyền **xin lượt**.
Sau khi người dùng chọn nguồn capture, frontend gọi
`POST /api/v1/meeting-runtime-sessions/{id}/screen-share?participantSid={sid}`.
Service kiểm tra membership, ban, trạng thái phiên, setting và connection SID hiện tại,
khóa bản ghi phiên bằng pessimistic write rồi kiểm tra quyền publish hiện tại trên LiveKit.
Chỉ một connection được cấp screen sources; request tranh lượt nhận
`error.meeting.screen-share.busy`. Frontend chờ sự kiện quyền rồi mới publish track.

Khi dừng hoặc publish thất bại, frontend gọi `DELETE` cùng đường dẫn/query để thu hồi
screen sources. SID ngăn cleanup của kết nối cũ ảnh hưởng kết nối mới. Khi connection
rời hẳn LiveKit, quyền gắn với connection biến mất. Admission và reconciliation cũng
giữ tối đa một người có screen sources. Không thêm bảng, field DB, Redis lock hay heartbeat.
Triển khai BE/FE cùng nhau và vào lại phòng để nhận token mới; không cần migration DB.

Ngoại lệ kiểm tra cho thay đổi này theo yêu cầu người dùng: không tạo/chạy test, không
chạy Docker; dùng Maven compile bỏ test và các kiểm tra format/type/build frontend tại máy local.

## 9. Kết thúc phiên học và dọn phòng

`POST /api/v1/meeting-runtime-sessions/{id}/end` dành cho OWNER/HOST, kết thúc phiên
ACTIVE và có thể gọi lại khi đã ENDED. Start/end/cancel khóa bản ghi phiên để tránh
tranh chấp với nhau và với lượt quét quá hạn. Phiên SCHEDULED quá giờ không được bắt
đầu; phiên ACTIVE được vào lại, trình chiếu và ghi hình trong thời gian dạy thêm.

Một scheduler chạy lượt đầu khi được khởi động, sau đó nghỉ 30 giây sau khi lượt trước
hoàn thành. Không gọi thêm bằng ApplicationReadyEvent. Scheduler dọn phiên chờ quá
giờ thành CANCELLED, thử lại cleanup và đồng bộ các phiên LIVE. Mỗi phòng LIVE lấy
một danh sách LiveKit dùng chung cho đồng bộ quyền và kiểm tra dạy thêm; không thêm
lượt gọi LiveKit riêng cho mỗi sinh viên hoặc scheduler thứ hai. Call timeout SDK là
10 giây. Lỗi lấy danh sách không được xem là phòng trống và không quyết định kết thúc.

Sau scheduled_end, giảng viên còn trong phòng thì phiên tiếp tục ACTIVE, kể cả sau
mốc scheduled_end + 1 giờ. Vắng giảng viên ít nhất 2 phút: nếu phòng trống thì kết
thúc; nếu còn sinh viên thì chỉ kết thúc khi đã qua mốc scheduled_end + 1 giờ. Ví dụ
lịch hết 10:00, giảng viên rời 11:20 thì kết thúc khoảng 11:22 ở lượt quét kế tiếp.
Trước scheduled_end không tự kết thúc vì phòng trống. Cả kết thúc thủ công và tự
động đều ghi actual_end bằng thời điểm kết thúc thực tế, không ghi đè scheduled_end.

Lớp gắn TKB xác định giảng viên bằng phân công hiện hành ở ClassSection; sinh viên
có role HOST không giữ lớp mở mãi. Phòng độc lập dùng thành viên OWNER/HOST còn hiệu
lực. Không dựa vào tên hoặc metadata role do client cung cấp. Redis giữ trạng thái
hiện diện trong một key `mtg:ha:{sessionId}`, TTL 24 giờ. Giá trị timestamp milliseconds
là lần đầu quan sát giảng viên vắng sau giờ; các lượt quét sau giữ nguyên mốc này.
Khi quan sát giảng viên có mặt hoặc webhook admission hợp lệ sau giờ, ghi marker
`p:{UUID}` mới để reset khoảng đệm và vô hiệu hóa snapshot cũ. Đọc trạng thái Redis
trước khi lấy danh sách LiveKit; Lua chỉ ghi/đọc mốc vắng nếu trạng thái vẫn khớp.
Do đó webhook vào lại vẫn vô hiệu hóa snapshot cũ dù không đổi `row_version` trong DB.
Phiên kết thúc xóa key trong cleanup sau commit. Backend restart dùng tiếp key còn
tồn tại; Redis mất key thì bắt đầu chờ lại 2 phút. Redis lỗi không được xem là đã
đủ khoảng đệm: bỏ qua xét dạy thêm và thử lại ở lượt quét tiếp theo. Đồng bộ quyền
LiveKit vẫn tiếp tục bằng cùng danh sách participant dù đọc Redis hoặc xét dạy thêm lỗi.
Kiểm tra version và khóa lại phiên trước khi đổi trạng thái. Việc lấy danh sách
LiveKit diễn ra trước transaction kiểm tra dạy thêm, không giữ khóa phiên khi chờ mạng.

`meeting_sessions.cleanup_pending` lưu công việc dọn còn dang dở, không
trả trường này qua DTO. Chuyển ENDED và đánh dấu pending trong cùng transaction.
Sau commit, service xóa phòng LiveKit trước, rồi dọn chat/giơ tay; chỉ bỏ cờ sau khi
tất cả thành công, kể cả xóa key hiện diện Redis. Lỗi được log và lượt quét sau thử
lại. LiveKit trả 404 được xem là đã xóa. Các bước có thể chạy lại sau crash, không
xóa điểm danh hoặc bản ghi.
Webhook người vào phiên ENDED bằng token cũ đánh dấu pending và kích hoạt lại dọn
phòng sau commit; không xem DeleteRoom là thu hồi token self-hosted.

Cleanup giữ khóa phiên trong lúc dọn để tuần tự hóa với webhook admission muộn:
lượt dọn cũ không được xóa cờ pending của một yêu cầu dọn mới. Đây là lựa chọn giữ
tính đúng đắn của retry; không đưa các lời gọi cleanup ra ngoài khóa nếu chưa có
cơ chế nhận biết từng lượt dọn. Việc lấy danh sách LiveKit để xét dạy thêm vẫn ở ngoài khóa.

Local `ddl-auto=update` tự thêm cột/index khi backend khởi động. Môi trường quản lý
schema thủ công chạy một lần `src/main/resources/db/sql/migrate_meeting_session_cleanup.sql`
trước khi triển khai; script cũng đánh dấu các phiên ENDED cũ để dọn lại. Không chạy
script thêm cột sau khi Hibernate đã tự thêm cùng cột/index. Hibernate chỉ cập nhật
schema, không tự chạy câu UPDATE đánh dấu phiên ENDED cũ; nếu cần dọn các phiên này
thì chạy riêng câu UPDATE trong script. Mốc vắng 2 phút chỉ lưu Redis, không thêm
cột hoặc migration dạy thêm. Nếu DB đã có cột mốc vắng từ bản triển khai trước,
cột đó không còn được mapping/sử dụng; Hibernate update không tự xóa cột cũ.
Không thêm field kỹ thuật vào DTO.

FE OWNER/HOST có lựa chọn Rời phòng hoặc Kết thúc cho tất cả, dùng API end hiện có.
Kết thúc thủ công đợi upload bản ghi trên máy người bấm; các máy ghi khác hoàn tất
upload khi disconnect/unmount, API upload/complete vẫn cho phép sau ENDED với đúng
chủ bản ghi và quyền hiện hành. Đã bỏ polling trong phòng sau giờ kết thúc: client
đang họp dựa vào LiveKit disconnect rồi refetch
trạng thái, không polling mỗi 5 giây trong suốt thời gian dạy thêm. Focus refresh và
kiểm tra phiên khi vào lại vẫn dùng API hiện có. Terminal status xóa marker reload.
Link vào lớp trên TKB vẫn hiện nếu session LIVE dù đã qua giờ dự kiến.

Theo yêu cầu người dùng, không giữ các file test bổ sung cho thay đổi chuyển mốc
vắng sang Redis. Đây là ngoại lệ có chủ đích với quy tắc bổ sung test trong
`docs/03-implementation-rules.md`.

TKB tạo/cập nhật entity session dùng chung qua luồng import hiện có. Field
`scheduleOccurrenceId` trên entity/response được giữ để tương thích dữ liệu và API.
API ban lưu lệnh cấm, loại participant sau commit, chặn token mới bằng
`requireNotBanned`, và kiểm tra lại token cũ qua webhook admission cùng đối soát
định kỳ. Ban chỉ áp dụng cho runtime session được chỉ định.
Kiểm tra lớp ACTIVE và giảng viên được gộp trong một truy vấn khi phân quyền thành viên;
vẫn kiểm tra ghi danh, ban và trạng thái tài khoản theo quyền hiện hành.
Theo yêu cầu không bổ sung lại test; đợt clean kiểm tra bằng Maven compile bỏ test,
rà soát call site và khởi động ứng dụng với H2 để xác nhận khởi tạo JPA/repository.
Các kiểm tra này không xác nhận kết nối LiveKit/Redis thực tế.
