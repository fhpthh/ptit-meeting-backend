# Phân tích Kiến trúc & Luồng Nghiệp vụ Module Notification (Phase 1 – Phase 3)

Tài liệu này mô tả toàn bộ kiến trúc, thiết kế cơ sở dữ liệu, các design patterns, sơ đồ luồng (flowchart/sequence), hợp đồng Input/Output và bộ câu hỏi phân tích chuyên sâu (phục vụ bảo vệ đồ án & phỏng vấn kỹ thuật) cho module `org.ptit.meeting.modules.notification`.

---

## 1. Tổng quan Kiến trúc & Ranh giới Module

Module `notification` được xây dựng theo kiến trúc **package-by-feature kết hợp 3-layer nhẹ** (`Controller -> Service -> Repository`), tách bạch 4 lớp trách nhiệm:
1. **Producers (Nguồn tạo thông báo)**: Phòng Giáo vụ (`NotificationAdminService`), Giảng viên (`LecturerNotificationService`), Scheduler nhắc lịch online (`OnlineClassReminderService`), và Event Listeners (`TimetableNotificationEventListener`).
2. **Shared Core Engine (Lõi phát hành dùng chung)**: `NotificationPublisherService` tập trung xử lý chống trùng (`dedupeKey`), validate giới hạn người nhận, lưu `Notification`, batch-insert hộp thư `NotificationRecipient`, tạo bản ghi `NotificationDelivery` và kích hoạt gửi kênh ngoài sau khi transaction commit.
3. **Channel Adapters (Kênh gửi)**: `NotificationDispatchService` điều phối qua interface `NotificationChannelAdapter` (`InAppNotificationChannelAdapter`, sẵn sàng cắm thêm `ZaloChannelAdapter`, Email, FCM mà không sửa core).
4. **Consumers (Người nhận)**: Hộp thư cá nhân (`NotificationInboxService`) và cài đặt cá nhân (`NotificationPreferenceService`).

```mermaid
flowchart LR
    subgraph Producers["1. Producers (Nguồn phát thông báo)"]
        PGV["PGV API\n/api/v1/admin/notifications"]
        LEC["Lecturer API\n/api/v1/lecturer/notifications"]
        SCHED["OnlineClassReminderScheduler\n(@Scheduled mỗi 60s)"]
        EVT["TimetableNotificationEventListener\n(@TransactionalEventListener AFTER_COMMIT)"]
    end

    subgraph CrossModule["Giao tiếp liên module (Read-only Service / Event)"]
        AUTH["auth.UserService"]
        TT_AUD["timetable.ClassSectionAudienceService"]
        TT_QRY["timetable.OnlineSessionReminderQueryService"]
    end

    subgraph Core["2. Notification Core Engine"]
        REG["AudienceResolverRegistry\n(Strategy Pattern)"]
        PUB["NotificationPublisherService\n(Dedupe + Batch Insert + afterCommit)"]
        DISP["NotificationDispatchService\n(Channel Dispatcher)"]
        ADAPT["NotificationChannelAdapter\n(IN_APP / ZALO / ...)"]
    end

    subgraph Storage["3. Persistence & Lock"]
        DB[("MySQL\nnotifications / recipients /\ndeliveries / preferences")]
        REDIS[("Redis\nDistributed Scheduler Lock")]
    end

    subgraph Consumers["4. Consumers (GV / SV)"]
        INBOX["NotificationController\n/api/v1/notifications"]
    end

    PGV --> REG --> AUTH
    REG --> TT_AUD
    LEC --> TT_AUD
    SCHED --> REDIS
    SCHED --> TT_QRY
    SCHED --> TT_AUD

    PGV --> PUB
    LEC --> PUB
    SCHED --> PUB
    EVT --> PUB

    PUB --> DB
    PUB -->|afterCommit| DISP --> ADAPT --> DB
    INBOX --> DB
```

### Nguyên tắc ranh giới module quan trọng
- **Không truy cập chéo repository**: `modules.notification` tuyệt đối không inject `UserRepository`, `ClassSectionRepository` hay `MeetingSessionRepository`. Mọi truy vấn dữ liệu đều đi qua service công khai (`UserService`, `ClassSectionAudienceService`, `OnlineSessionReminderQueryService`).
- **Không tạo circular dependency**: `timetable` không gọi trực tiếp `notification`. Khi PGV phân công giảng viên hoặc ghi danh sinh viên vào lớp online, `timetable` chỉ phát Spring `ApplicationEvent` (`LecturerAssignedEvent`, `StudentEnrolledInClassSectionEvent`), còn `notification` tự lắng nghe.

---

## 2. Design Patterns & Nguyên tắc SOLID / DRY áp dụng

| Pattern / Nguyên tắc | Class / Interface trong code | Giải quyết vấn đề gì? |
|---|---|---|
| **Strategy Pattern** (OCP) | `AudienceResolver`, `AudienceResolverRegistry`, 5 resolver (`AllUsers`, `Role`, `Department`, `ClassSection`, `ExplicitUsers`) | PGV có nhiều phạm vi gửi khác nhau. Muốn thêm phạm vi mới (VD: theo Khóa `COHORT` hay Lớp hành chính) chỉ cần thêm 1 `@Component` mới, **không sửa `if/else`** trong `NotificationAdminServiceImpl`. |
| **Adapter Pattern** (DIP/OCP) | `NotificationChannelAdapter`, `InAppNotificationChannelAdapter`, `NotificationDispatchServiceImpl` | Tách biệt logic phát hành khỏi kỹ thuật từng kênh gửi. Phase 4 thêm Zalo Bot chỉ cần tạo `ZaloChannelAdapter` implement `NotificationChannelAdapter`. |
| **Template Method Pattern** (DRY) | `AbstractAnnouncementService` (cha của `NotificationAdminServiceImpl` & `LecturerNotificationServiceImpl`) | Gom toàn bộ logic dùng chung: chuẩn hóa phân trang, tìm kiếm kèm thống kê đã đọc, xem chi tiết, thu hồi (soft-delete). Lớp con chỉ override `verifyAccess(...)` theo đúng quyền vai trò. |
| **Single Responsibility (SRP)** | `NotificationPublisherServiceImpl`, `NotificationInboxServiceImpl`, `NotificationPreferenceServiceImpl`, `OnlineClassReminderServiceImpl` | Mỗi service chịu trách nhiệm đúng 1 vòng đời nghiệp vụ, dễ viết unit test độc lập. |
| **Externalized Config (No Hardcode)** | `NotificationProperties` (`application.yml`), `NotificationErrorConstants`, `i18n/messages*.properties` | Toàn bộ ngưỡng batch size, max users, min/max phút nhắc (`30..120`), lock TTL, timezone và nội dung thông báo đều cấu hình được và hỗ trợ đa ngôn ngữ (`vi`/`en`). |
| **Deterministic Time (Testability)** | Inject `java.time.Clock` vào `OnlineClassReminderServiceImpl` | Unit test kiểm tra chính xác cửa sổ 30 phút / 45 phút mà không phụ thuộc đồng hồ hệ thống. |

---

## 3. Thiết kế Cơ sở dữ liệu (ER Diagram)

```mermaid
erDiagram
    NOTIFICATIONS ||--o{ NOTIFICATION_RECIPIENTS : "1-N (hộp thư từng user)"
    NOTIFICATIONS ||--o{ NOTIFICATION_DELIVERIES : "1-N (trạng thái gửi kênh ngoài)"

    NOTIFICATIONS {
        varchar id PK "UUID (BaseEntity)"
        varchar type "PGV_ANNOUNCEMENT | LECTURER_ANNOUNCEMENT | CLASS_REMINDER | SCHEDULE_CHANGE"
        varchar category "GENERAL | ACADEMIC | TIMETABLE | COURSE | URGENT"
        varchar priority "NORMAL | HIGH | URGENT"
        varchar title
        text content
        varchar target_scope "ALL | ROLE | DEPARTMENT | CLASS_SECTION | USERS"
        varchar target_ref
        varchar reference_type "MEETING_SESSION | CLASS_SECTION | PGV_ANNOUNCEMENT"
        varchar reference_id "UUID / ID để FE điều hướng"
        varchar dedupe_key UK "Chống gửi trùng"
        bigint created_by "user_id người gửi (null nếu hệ thống)"
        timestamp published_at
        boolean is_deleted "Soft-delete khi thu hồi"
        timestamp deleted_at
    }

    NOTIFICATION_RECIPIENTS {
        varchar id PK "UUID"
        varchar notification_id FK
        bigint user_id "UK(notification_id, user_id)"
        boolean is_read
        timestamp read_at
        timestamp created_at
    }

    NOTIFICATION_DELIVERIES {
        varchar id PK "UUID"
        varchar notification_id FK
        bigint user_id
        varchar channel "IN_APP | ZALO"
        varchar destination "zalo_user_id / chat_id"
        varchar status "PENDING | SENT | FAILED | SKIPPED"
        int attempt_count
        timestamp next_retry_at
        varchar provider_message_id
        varchar error_code
        varchar error_message
        timestamp sent_at
    }

    NOTIFICATION_PREFERENCES {
        varchar id PK "UUID"
        bigint user_id UK
        boolean in_app_enabled
        boolean zalo_enabled
        int reminder_minutes_before "Mặc định 30, khoảng 30..120"
        boolean receive_pgv_via_zalo
        boolean receive_lecturer_via_zalo
    }
```

**Vì sao tách `notifications` và `notification_recipients`?**
- Khi PGV gửi thông báo toàn trường (VD: 10.000 SV), nội dung `title`/`content` chỉ lưu **1 bản ghi duy nhất** ở `notifications`, còn `notification_recipients` chỉ lưu `(notification_id, user_id, is_read, read_at)` → tiết kiệm dung lượng DB, thống kê `readCount`/`totalRecipients` cực nhanh, và thu hồi thông báo chỉ cần update `is_deleted = true` ở đúng 1 dòng `notifications`.

---

## 4. Phân tích chi tiết 5 Luồng Nghiệp vụ (Flowchart + Input/Output)

### Luồng 1: Phòng Giáo vụ gửi thông báo (`POST /api/v1/admin/notifications`)

```mermaid
flowchart TD
    A["Client PGV (ROLE_STAFF / ROLE_ADMIN)\nPOST /api/v1/admin/notifications"] --> B{"Spring Security\nhasAnyRole(STAFF, ADMIN)?"}
    B -- "Không" --> E403["HTTP 403 Forbidden"]
    B -- "Có" --> C{"@Valid Request DTO\n(title, content, targetScope)"}
    C -- "Lỗi" --> E400["HTTP 400 + ApiError (i18n)"]
    C -- "Hợp lệ" --> D["AudienceResolverRegistry.resolve(scope)"]
    D --> D1["ALL -> UserService (STUDENT + LECTURER)"]
    D --> D2["ROLE -> UserService (STUDENT hoặc LECTURER)"]
    D --> D3["DEPARTMENT -> UserService (theo khoa)"]
    D --> D4["CLASS_SECTION -> ClassSectionAudienceService"]
    D --> D5["USERS -> UserService (danh sách userIds active)"]
    D1 & D2 & D3 & D4 & D5 --> E["NotificationPublisherService.publish(command)"]
    E --> F{"recipients rỗng hoặc\n> maxTargetUsers (10.000)?"}
    F -- "Có" --> E400B["BusinessException 400"]
    F -- "Không" --> G["1. Save Notification\n2. Batch insert NotificationRecipient (chunk 500)\n3. Nếu có kênh ngoài -> Save NotificationDelivery (PENDING)"]
    G --> H["Transaction Commit\n-> afterCommit: NotificationDispatchService"]
    H --> I["HTTP 200 BaseResponse<AnnouncementResponse>"]
```

- **Input mẫu (`AdminAnnouncementCreateRequest`)**:
```json
{
  "title": "Thông báo lịch thi giữa kỳ",
  "content": "Sinh viên kiểm tra lịch thi từ ngày 01/12.",
  "category": "ACADEMIC",
  "priority": "HIGH",
  "targetScope": "ROLE",
  "targetRole": "STUDENT",
  "channels": ["IN_APP"]
}
```
- **Output mẫu (`BaseResponse<AnnouncementResponse>`)**:
```json
{
  "code": "0",
  "strCode": "success",
  "message": "SUCCESSFUL",
  "content": {
    "id": "b1f2c3d4-...",
    "type": "PGV_ANNOUNCEMENT",
    "category": "ACADEMIC",
    "priority": "HIGH",
    "title": "Thông báo lịch thi giữa kỳ",
    "targetScope": "ROLE",
    "targetRef": "STUDENT",
    "createdBy": 1,
    "totalRecipients": 1250,
    "readCount": 0
  }
}
```

---

### Luồng 2: Giảng viên gửi thông báo tới lớp phụ trách (`POST /api/v1/lecturer/notifications`)

```mermaid
flowchart TD
    A["Client Giảng viên (ROLE_LECTURER)\nPOST /api/v1/lecturer/notifications"] --> B["SecurityConfig: /api/v1/lecturer/**"]
    B --> C["LecturerNotificationService.createAnnouncement(lecturerId, req)"]
    C --> D["ClassSectionAudienceService.findOwnedClassSectionStudentIds(lecturerId, sectionIds)"]
    D --> E{"Tất cả lớp tồn tại,\nACTIVE và lecturerId == currentUser?"}
    E -- "Không tồn tại" --> E404["HTTP 404 CLASS_SECTION_NOT_FOUND"]
    E -- "Không phụ trách" --> E403["HTTP 403 Unauthorized"]
    E -- "Hợp lệ" --> F{"Request có truyền studentIds?"}
    F -- "Không" --> G["Lấy toàn bộ SV ENROLLED + ACTIVE của các lớp"]
    F -- "Có" --> H{"Mọi studentId đều thuộc\ndanh sách ENROLLED của lớp?"}
    H -- "Không" --> E400["HTTP 400 STUDENT_NOT_IN_CLASS_SECTIONS"]
    H -- "Có" --> G2["Lấy đúng tập con studentIds"]
    G & G2 --> I["Loại trừ chính lecturerId\nGọi NotificationPublisherService.publish(LECTURER_ANNOUNCEMENT)"]
    I --> J["HTTP 200 BaseResponse<AnnouncementResponse>"]
```

- **Input mẫu (`LecturerAnnouncementCreateRequest`)**:
```json
{
  "title": "Nhắc nộp bài tập thực hành buổi 3",
  "content": "Các em nộp bài trước 23:59 tối nay.",
  "category": "COURSE",
  "priority": "NORMAL",
  "classSectionIds": [10],
  "studentIds": [201, 202],
  "channels": ["IN_APP"]
}
```
- **Kiểm soát quyền xem/thu hồi**: Khi giảng viên gọi `GET` hoặc `DELETE /api/v1/lecturer/notifications/{id}`, `verifyAccess(...)` kiểm tra bắt buộc `type == LECTURER_ANNOUNCEMENT` và `createdBy.equals(lecturerUserId)`.

---

### Luồng 3: Hộp thư cá nhân & Cài đặt (`GET /api/v1/notifications`)

- **Đọc hộp thư (`GET /api/v1/notifications?type=&category=&read=&page=0&size=20`)**:
  - Join `NotificationRecipient r` với `Notification n` nơi `r.userId = currentUserId` và `n.deleted = false`.
  - Trả về `PageResponseDTO<UserNotificationResponse>` gồm `recipientId`, `notificationId`, `type`, `title`, `content`, `referenceType`, `referenceId`, `read`, `readAt`, `publishedAt`.
- **Đếm chưa đọc (`GET /api/v1/notifications/unread-count`)**: Trả `{"unreadCount": 3}` (dùng index `(user_id, is_read, created_at)`).
- **Đánh dấu đã đọc**:
  - `PATCH /api/v1/notifications/{recipientId}/read`: tìm theo `(recipientId, currentUserId)` → chống IDOR (user A không thể đánh dấu đọc hộp thư của user B).
  - `PATCH /api/v1/notifications/read-all`: 1 câu bulk `UPDATE` trả về `{"markedCount": N}`.
- **Cài đặt cá nhân (`GET/PUT /api/v1/notifications/preferences`)**:
  - Nếu user chưa có bản ghi, trả cấu hình mặc định từ `NotificationProperties` (`reminderMinutesBefore = 30`).
  - Khi cập nhật, validate cứng `minReminderMinutes (30) <= reminderMinutesBefore <= maxReminderMinutes (120)`.

---

### Luồng 4: Scheduler nhắc lịch học trực tuyến tự động trước tối thiểu 30 phút

```mermaid
flowchart TD
    T["@Scheduled mỗi 60s\nOnlineClassReminderScheduler"] --> E{"reminderEnabled == true?"}
    E -- "False" --> Z["Dừng"]
    E -- "True" --> L["RedisNotificationSchedulerLock.tryAcquire(lockKey, 55s)"]
    L --> L1{"Lấy được lock?"}
    L1 -- "Không (node khác đang chạy)" --> Z
    L1 -- "Có (hoặc local fallback)" --> Q["now = LocalDateTime.now(Asia/Ho_Chi_Minh)\nQuery OnlineSessionReminderQueryService:\nMeetingSession SCHEDULED + isOnline=true\ntrong (now, now + 120 phút]"]
    Q --> LOOP["Với mỗi buổi học online sắp diễn ra"]
    LOOP --> M["Lấy thành viên active: GV phụ trách + SV ENROLLED"]
    M --> IDEM["Lọc bỏ user đã nằm trong\nfindNotifiedUserIds(CLASS_REMINDER, MEETING_SESSION, sessionPublicId)"]
    IDEM --> PREF["Batch-load NotificationPreference của các user còn lại"]
    PREF --> WIN["Với từng user:\nlead = clamp(pref.reminderMinutesBefore, 30, 120)\nNếu scheduledStart <= now + lead -> Đủ điều kiện nhắc"]
    WIN --> GRP["Gom nhóm theo (leadMinutes, channels)\nFormat title/content từ MessageSource (i18n)"]
    GRP --> PUB["NotificationPublisherService.publish(\ndedupeKey = ONLINE_CLASS_REMINDER:sessionId:lead:channels)"]
    PUB --> UNLOCK["finally: RedisNotificationSchedulerLock.release(lockKey, token)"]
```

**Cơ chế chống gửi trùng (Idempotency) 3 lớp**:
1. **Redis Distributed Lock (`SET NX PX`)**: Ngăn nhiều instance backend cùng quét tại cùng một thời điểm; giải phóng khóa bằng Lua script kiểm tra đúng `token` sở hữu.
2. **Per-User Session Check (`findNotifiedUserIds`)**: Kiểm tra user đã được nhắc cho `sessionPublicId` chưa. Nhờ vậy, nếu user đã được nhắc lúc trước 45 phút rồi đổi cài đặt xuống 30 phút, hệ thống vẫn **không bao giờ nhắc lại lần 2**.
3. **Database Unique Constraint (`dedupe_key`)**: Tầng bảo vệ cuối cùng ở DB chống race condition.

---

### Luồng 5: Event Triggers tự động từ module `timetable`

```mermaid
sequenceDiagram
    participant PGV as Phòng Giáo vụ
    participant TT as Timetable Service<br/>(SemesterSubjectAdmin / StudentEnrollment)
    participant DB as MySQL
    participant EV as Spring ApplicationEventPublisher
    participant LST as TimetableNotificationEventListener
    participant PUB as NotificationPublisherService

    PGV->>TT: Phân công GV / Import DSSV vào lớp học phần
    activate TT
    TT->>DB: Lưu ClassSection / ClassEnrollment (TX 1)
    TT->>DB: Kiểm tra lớp có lịch online (existsByClassSectionIdAndIsOnlineTrue)
    alt Lớp có lịch online
        TT->>EV: publishEvent(LecturerAssignedEvent / StudentEnrolledEvent)
    end
    TT-->>DB: Commit TX 1
    deactivate TT

    Note over EV,LST: Chỉ kích hoạt sau khi TX 1 commit thành công (AFTER_COMMIT)
    EV->>LST: onLecturerAssigned / onStudentsEnrolled
    activate LST
    LST->>PUB: publish(SCHEDULE_CHANGE) trong TX 2 (REQUIRES_NEW)
    PUB->>DB: Lưu Notification + NotificationRecipient
    deactivate LST
```

---

### Luồng 6: Đẩy thông báo Realtime bằng Server-Sent Events (SSE)

```mermaid
flowchart TD
    A["Browser (GV / SV đã đăng nhập)\nGET /api/v1/notifications/stream\nHeader: Authorization Bearer JWT"] --> B["JwtAuthenticationFilter\nXác thực JWT -> lấy userId"]
    B --> C["NotificationRealtimeService.subscribe(userId)"]
    C --> D{"Số kết nối của userId\n>= maxSseConnectionsPerUser (5)?"}
    D -- "Có" --> E["Đóng & loại bỏ SseEmitter cũ nhất\n(chống leak bộ nhớ khi mở nhiều tab)"]
    D -- "Không" --> F["Lưu SseEmitter vào\nConcurrentHashMap<userId, CopyOnWriteArrayList>"]
    E --> F
    F --> G["Đăng ký callback dọn dẹp:\nonCompletion / onTimeout / onError"]
    G --> H["Gửi ngay event: connected\n(ép proxy flush HTTP headers)"]

    subgraph PushFlow["Khi có thông báo mới (PGV / GV / Scheduler / Event)"]
        P1["NotificationPublisherService.publish()"] --> P2["Lưu MySQL (Notification + Recipients)"]
        P2 --> P3["Transaction Commit\n-> afterCommit callback"]
        P3 --> P4["NotificationRealtimeService.pushToUsers()"]
        P4 --> P5{"userId có đang mở\nkết nối SSE?"}
        P5 -- "Không (Offline)" --> P6["Bỏ qua O(1)\n(User xem lại qua REST khi online)"]
        P5 -- "Có (Online)" --> P7["Gửi event: notification\n(NotificationRealtimePayload JSON)"]
        P7 --> P8{"Gửi thành công?"}
        P8 -- "IOException (client đã đóng tab)" --> P9["Remove & complete emitter"]
        P8 -- "OK" --> P10["FE nhận event < 1s:\nTăng badge chuông + bật Toast popup"]
    end

    subgraph KeepAlive["Giữ kết nối (NotificationSseHeartbeatScheduler)"]
        K1["@Scheduled mỗi 25s"] --> K2["Gửi event: heartbeat tới mọi emitter\n(tránh Nginx/Proxy cắt kết nối idle)"]
    end
```

---

## 5. Bộ câu hỏi Phỏng vấn thực chiến & Cách trả lời

### Câu 1: Làm sao hệ thống đảm bảo không gửi trùng thông báo nhắc lịch khi scale lên nhiều server (multi-instance)?
> **Trả lời**: Hệ thống dùng cơ chế phòng thủ **3 lớp**:
> 1. **Tầng điều phối**: Redis Distributed Lock (`SET key token NX PX ttl`) kèm Lua script unlock an toàn theo `token`, đảm bảo tại một thời điểm chỉ 1 instance quét lịch.
> 2. **Tầng nghiệp vụ**: Truy vấn `findNotifiedUserIds` theo `(CLASS_REMINDER, MEETING_SESSION, sessionPublicId)` để loại bỏ những user đã được nhắc (tránh nhắc lặp ngay cả khi user đổi cấu hình số phút nhắc).
> 3. **Tầng dữ liệu**: Cột `dedupe_key` có `UNIQUE constraint` trong bảng `notifications`, và `UNIQUE(notification_id, user_id)` trong `notification_recipients`.

### Câu 2: Khi Phòng Giáo vụ gửi thông báo tới 10.000 sinh viên, làm sao tránh treo HTTP request và tối ưu ghi DB?
> **Trả lời**:
> - Tách bảng `notifications` (1 dòng nội dung gốc) và `notification_recipients` (chỉ chứa FK + trạng thái đọc), tránh nhân bản `TEXT` content 10.000 lần.
> - Insert `notification_recipients` theo **batch** (chunk 500 cấu hình qua `NotificationProperties`, kết hợp `hibernate.jdbc.batch_size: 50` và `rewriteBatchedStatements=true` trên MySQL).
> - Có ngưỡng bảo vệ `maxTargetUsers` tránh OOM.
> - Gửi kênh ngoài (Zalo) chỉ chạy **sau khi DB transaction commit** (`TransactionSynchronizationManager.afterCommit`), không giữ DB connection trong lúc gọi HTTP API bên thứ ba (và ở Phase 4 có thể kết hợp `@Async` / retry scheduler).

### Câu 3: Tại sao Event Listener dùng `@TransactionalEventListener(AFTER_COMMIT)` kết hợp `@Transactional(REQUIRES_NEW)`?
> **Trả lời**:
> - Nếu dùng `@EventListener` thông thường chạy trong cùng transaction của `timetable`, khi transaction `timetable` bị rollback thì thông báo đã có thể bị phát nhầm; ngược lại nếu gửi thông báo lỗi thì lại làm rollback cả đợt import danh sách sinh viên của PGV.
> - `AFTER_COMMIT` đảm bảo chỉ gửi thông báo khi dữ liệu TKB đã lưu chắc chắn. Vì lúc đó TX 1 đã đóng, cần `REQUIRES_NEW` để mở TX 2 riêng cho việc lưu thông báo (và bọc `try/catch` log lỗi để không ảnh hưởng luồng chính).

### Câu 4: Tại sao không để `notification` gọi thẳng `UserRepository` hoặc `ClassSectionRepository` cho nhanh?
> **Trả lời**: Để giữ **encapsulation & loose coupling** giữa các module theo kiến trúc package-by-feature. Nếu `notification` chọc thẳng vào repository của `timetable` hay `auth`, mọi thay đổi schema/business rule (như điều kiện lớp `ACTIVE`, sinh viên `ENROLLED`, quyền phụ trách của giảng viên) sẽ bị nhân bản rải rác. Đi qua service công khai (`ClassSectionAudienceService`, `UserService`) giúp gom business rule vào đúng module sở hữu và dễ tách microservice sau này.

### Câu 5: Nếu sau này sếp yêu cầu thêm kênh gửi Email/Zalo hoặc thêm phạm vi gửi theo "Khóa tuyển sinh", bạn phải sửa code ở những đâu?
> **Trả lời**: Nhờ tuân thủ **Open/Closed Principle**:
> - Thêm kênh gửi mới (Zalo/Email): chỉ cần thêm enum vào `DeliveryChannel`, viết client kỹ thuật ở `integration.<provider>` và tạo 1 class implement `NotificationChannelAdapter` — **không sửa** `NotificationPublisherServiceImpl` hay `NotificationDispatchServiceImpl`.
> - Thêm phạm vi đối tượng mới (`COHORT`): chỉ cần thêm 1 class implement `AudienceResolver` — `AudienceResolverRegistry` tự động đăng ký qua Spring DI, **không sửa** `NotificationAdminServiceImpl`.

### Câu 6: Giảng viên có thể giả mạo request gửi thông báo tới lớp của giảng viên khác hoặc sinh viên không thuộc lớp mình không?
> **Trả lời**: Không, vì có **3 lớp kiểm tra**:
> 1. `SecurityConfig` chặn URL `/api/v1/lecturer/**` yêu cầu JWT có `ROLE_LECTURER`/`STAFF`/`ADMIN`.
> 2. `ClassSectionAudienceService.findOwnedClassSectionStudentIds` xác minh mọi `classSectionId` đều tồn tại, `ACTIVE` và có `lecturerId == currentUserId` (sai → `403`).
> 3. Nếu giảng viên truyền thêm `studentIds`, `LecturerNotificationServiceImpl` kiểm tra từng `studentId` có nằm trong tập sinh viên `ENROLLED` của đúng các lớp đó không (sai → `400`).

### Câu 7: Nếu Redis bị chết đột ngột thì Scheduler nhắc lịch có bị sập toàn bộ không?
> **Trả lời**: `RedisNotificationSchedulerLock` bắt `DataAccessException`, ghi log cảnh báo và trả về `LOCAL_FALLBACK` để scheduler vẫn chạy được ở chế độ đơn node thay vì bỏ lỡ nhắc lịch học của sinh viên; đồng thời lớp `findNotifiedUserIds` + `dedupe_key` ở MySQL vẫn tiếp tục bảo vệ chống gửi trùng.

### Câu 8: Làm sao bạn unit test các logic phụ thuộc thời gian thực (nhắc trước 30 phút / 45 phút)?
> **Trả lời**: `OnlineClassReminderServiceImpl` nhận `java.time.Clock` qua constructor (Spring dùng `Clock.systemDefaultZone()`, còn test truyền `Clock.fixed(...)`). Nhờ đó test chạy cực nhanh, hoàn toàn deterministic (ví dụ cố định đồng hồ ở `08:20` cho buổi học `09:00` để xác minh user cài `45 phút` được nhắc còn user mặc định `30 phút` chưa bị nhắc sớm).

### Câu 9: Tại sao chọn Server-Sent Events (SSE) thay vì WebSocket hay HTTP Polling cho Notification?
> **Trả lời**:
> - **So với Polling**: Polling gọi HTTP lặp lại liên tục gây tốn băng thông, tải DB cao và có độ trễ (30–60s). SSE đẩy tức thì (< 1s) khi có sự kiện thật.
> - **So với WebSocket**: WebSocket là giao tiếp 2 chiều (full-duplex) qua giao thức `ws://`, nặng hơn và cần cấu hình riêng (STOMP/broker, xử lý firewall/proxy phức tạp). Thông báo chỉ cần **1 chiều Server → Client**, nên SSE chạy ngay trên **HTTP/1.1 hoặc HTTP/2 chuẩn**, giữ nguyên cơ chế bảo mật JWT filter hiện có và cực nhẹ.

### Câu 10: SSE quản lý bộ nhớ, kết nối chết và bài toán scale nhiều server như thế nào?
> **Trả lời**:
> - **Chống memory leak**: Dùng `ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>>`, giới hạn `maxSseConnectionsPerUser = 5` (mở quá tự đẩy tab cũ nhất), đăng ký đủ `onCompletion`/`onTimeout`/`onError`, và chạy `heartbeat` mỗi 25s để vừa chống Nginx cắt kết nối idle vừa phát hiện sớm `IOException` của tab đã đóng.
> - **Tránh đẩy dữ liệu chưa commit**: Chỉ gọi `realtimeService.pushToUsers(...)` trong `TransactionSynchronization.afterCommit()`.
> - **Khi scale nhiều instance backend**: Nếu User A kết nối SSE ở Server 1 nhưng PGV gọi API gửi ở Server 2, chỉ cần bổ sung **Redis Pub/Sub** (Server 2 publish payload lên channel Redis, mọi server subscribe nhận và đẩy xuống `SseEmitter` nội bộ nếu user đó đang kết nối trên mình).
