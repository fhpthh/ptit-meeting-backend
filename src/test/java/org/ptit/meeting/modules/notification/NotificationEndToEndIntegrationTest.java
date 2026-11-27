package org.ptit.meeting.modules.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.repository.UserRepository;
import org.ptit.meeting.modules.auth.security.JwtTokenProvider;
import org.ptit.meeting.modules.auth.service.TokenService;
import org.ptit.meeting.modules.meeting.constant.enums.RoomStatus;
import org.ptit.meeting.modules.meeting.constant.enums.RoomType;
import org.ptit.meeting.modules.meeting.constant.enums.SessionStatus;
import org.ptit.meeting.modules.meeting.entity.MeetingSession;
import org.ptit.meeting.modules.meeting.entity.Room;
import org.ptit.meeting.modules.notification.constant.enums.NotificationType;
import org.ptit.meeting.modules.notification.repository.NotificationDeliveryRepository;
import org.ptit.meeting.modules.notification.repository.NotificationPreferenceRepository;
import org.ptit.meeting.modules.notification.repository.NotificationRecipientRepository;
import org.ptit.meeting.modules.notification.repository.NotificationRepository;
import org.ptit.meeting.modules.notification.service.OnlineClassReminderService;
import org.ptit.meeting.modules.timetable.constant.enums.ClassSectionStatus;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;
import org.ptit.meeting.modules.timetable.entity.ClassSchedule;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.ptit.meeting.modules.timetable.entity.Semester;
import org.ptit.meeting.modules.timetable.entity.Subject;
import org.ptit.meeting.modules.timetable.repository.ClassEnrollmentRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSchedulerRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSectionRepository;
import org.ptit.meeting.modules.timetable.repository.MeetingSessionRepository;
import org.ptit.meeting.modules.timetable.repository.RoomRepository;
import org.ptit.meeting.modules.timetable.repository.SemesterRepository;
import org.ptit.meeting.modules.timetable.repository.SubjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:notification_e2e_integration;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.liquibase.enabled=false",
    "spring.sql.init.mode=never",
    "application.livekit.enabled=false",
    "application.recording.enabled=false",
    "application.notification.reminder-enabled=false",
    "application.security.jwt.secret-key=notification-e2e-integration-test-key-0123456789"
})
@AutoConfigureMockMvc
class NotificationEndToEndIntegrationTest {

  private static final ZoneId SCHEDULE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  @Autowired private MockMvc mvc;
  @Autowired private JwtTokenProvider jwt;
  @Autowired private UserRepository userRepository;
  @Autowired private SemesterRepository semesterRepository;
  @Autowired private SubjectRepository subjectRepository;
  @Autowired private ClassSectionRepository classSectionRepository;
  @Autowired private ClassSchedulerRepository classSchedulerRepository;
  @Autowired private ClassEnrollmentRepository classEnrollmentRepository;
  @Autowired private RoomRepository roomRepository;
  @Autowired private MeetingSessionRepository meetingSessionRepository;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private NotificationRecipientRepository recipientRepository;
  @Autowired private NotificationDeliveryRepository deliveryRepository;
  @Autowired private NotificationPreferenceRepository preferenceRepository;
  @Autowired private OnlineClassReminderService onlineClassReminderService;

  @MockitoBean private TokenService tokenService;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private User staffUser;
  private User lecturerA;
  private User lecturerB;
  private User student1;
  private User student2;
  private ClassSection onlineSection;
  private MeetingSession upcomingOnlineSession;

  @BeforeEach
  void setUp() {
    deliveryRepository.deleteAll();
    recipientRepository.deleteAll();
    notificationRepository.deleteAll();
    preferenceRepository.deleteAll();
    meetingSessionRepository.deleteAll();
    roomRepository.deleteAll();
    classEnrollmentRepository.deleteAll();
    classSchedulerRepository.deleteAll();
    classSectionRepository.deleteAll();
    subjectRepository.deleteAll();
    semesterRepository.deleteAll();
    userRepository.deleteAll();

    staffUser = saveUser("pgv@ptit.edu.vn", "PGV01", "Can bo PGV", AccountType.STAFF, "PGV");
    lecturerA = saveUser("gva@ptit.edu.vn", "GV01", "Giang vien A", AccountType.LECTURER, "CNTT");
    lecturerB = saveUser("gvb@ptit.edu.vn", "GV02", "Giang vien B", AccountType.LECTURER, "CNTT");
    student1 = saveUser("sv1@stu.ptit.edu.vn", "B22DCCN001", "Sinh vien 1", AccountType.STUDENT, "D22CN01");
    student2 = saveUser("sv2@stu.ptit.edu.vn", "B22DCCN002", "Sinh vien 2", AccountType.STUDENT, "D22CN01");

    Semester semester = semesterRepository.save(Semester.builder()
        .code("2026-2027-HK1")
        .name("Hoc ky 1 2026-2027")
        .academicYear("2026-2027")
        .semesterNum(1)
        .startDate(LocalDate.now(SCHEDULE_ZONE).minusWeeks(2))
        .endDate(LocalDate.now(SCHEDULE_ZONE).plusWeeks(12))
        .isActive(true)
        .build());

    Subject subject = subjectRepository.save(Subject.builder()
        .code("INT1434")
        .name("Lap trinh Web")
        .credit(3)
        .department("CNTT")
        .build());

    onlineSection = classSectionRepository.save(ClassSection.builder()
        .semesterId(semester.getId())
        .subjectId(subject.getId())
        .lecturerId(lecturerA.getId())
        .lecturerName(lecturerA.getFullName())
        .cohort("D22")
        .classCode("INT1434-01")
        .groupNumber("01")
        .status(ClassSectionStatus.ACTIVE)
        .build());

    ClassSchedule onlineSchedule = classSchedulerRepository.save(ClassSchedule.builder()
        .classSectionId(onlineSection.getId())
        .dayOfWeek(2)
        .startPeriod(1)
        .numPeriods(2)
        .startTime(LocalTime.of(7, 0))
        .endTime(LocalTime.of(8, 50))
        .weeksPattern("123456789")
        .isOnline(true)
        .build());

    Room onlineRoom = roomRepository.save(Room.builder()
        .publicId(UUID.randomUUID().toString())
        .code("ROOM-INT1434-01")
        .name("Phong hoc truc tuyen INT1434-01")
        .roomType(RoomType.CLASS)
        .classSectionId(onlineSection.getId())
        .status(RoomStatus.ACTIVE)
        .build());

    LocalDateTime startIn25Minutes = LocalDateTime.now(SCHEDULE_ZONE).plusMinutes(25);
    upcomingOnlineSession = meetingSessionRepository.save(MeetingSession.builder()
        .publicId(UUID.randomUUID().toString())
        .title("Online class INT1434-01")
        .providerRoomName("livekit-" + UUID.randomUUID().toString().substring(0, 8))
        .roomId(onlineRoom.getId())
        .scheduleId(onlineSchedule.getId())
        .studyDate(startIn25Minutes.toLocalDate())
        .scheduledStart(startIn25Minutes)
        .scheduledEnd(startIn25Minutes.plusMinutes(110))
        .status(SessionStatus.SCHEDULED)
        .build());

    classEnrollmentRepository.saveAll(List.of(
        ClassEnrollment.builder()
            .classSectionId(onlineSection.getId())
            .userId(student1.getId())
            .status(EnrollmentStatus.ENROLLED)
            .build(),
        ClassEnrollment.builder()
            .classSectionId(onlineSection.getId())
            .userId(student2.getId())
            .status(EnrollmentStatus.ENROLLED)
            .build()
    ));
  }

  @Test
  void fullPhase1To3EndToEndFlow_worksAcrossRolesInboxPreferencesAndOnlineReminders()
      throws Exception {
    String staffToken = bearer(staffUser, Set.of("ROLE_STAFF"));
    String lecturerAToken = bearer(lecturerA, Set.of("ROLE_LECTURER"));
    String lecturerBToken = bearer(lecturerB, Set.of("ROLE_LECTURER"));
    String student1Token = bearer(student1, Set.of("ROLE_STUDENT"));

    // 1. Security checks: unauthenticated & cross-role forbidden
    mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/admin/notifications")
            .header(HttpHeaders.AUTHORIZATION, student1Token))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/lecturer/notifications")
            .header(HttpHeaders.AUTHORIZATION, lecturerBToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "Unauthorized class notice",
                  "content": "Should fail",
                  "classSectionIds": [%d]
                }
                """.formatted(onlineSection.getId())))
        .andExpect(status().isForbidden());

    // 2. PGV sends announcement to all STUDENT users
    MvcResult pgvCreateResult = mvc.perform(post("/api/v1/admin/notifications")
            .header(HttpHeaders.AUTHORIZATION, staffToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "Thong bao lich thi giua ky",
                  "content": "Sinh vien xem lich thi tren cong dao tao.",
                  "category": "ACADEMIC",
                  "priority": "HIGH",
                  "targetScope": "ROLE",
                  "targetRole": "STUDENT",
                  "channels": ["IN_APP"]
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.type").value("PGV_ANNOUNCEMENT"))
        .andExpect(jsonPath("$.content.totalRecipients").value(2))
        .andReturn();
    String pgvNotificationId = readJson(pgvCreateResult).path("content").path("id").asText();

    // 3. Lecturer A sends announcement to owned online class section
    MvcResult lecCreateResult = mvc.perform(post("/api/v1/lecturer/notifications")
            .header(HttpHeaders.AUTHORIZATION, lecturerAToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "title": "Nhac nop bai tap tuan 3",
                  "content": "Han chot 23:59 hom nay.",
                  "category": "COURSE",
                  "priority": "NORMAL",
                  "classSectionIds": [%d]
                }
                """.formatted(onlineSection.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.type").value("LECTURER_ANNOUNCEMENT"))
        .andExpect(jsonPath("$.content.totalRecipients").value(2))
        .andReturn();
    String lecturerNotificationId = readJson(lecCreateResult).path("content").path("id").asText();

    // 4. Student 1 checks unread count (2) and inbox
    mvc.perform(get("/api/v1/notifications/unread-count")
            .header(HttpHeaders.AUTHORIZATION, student1Token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.unreadCount").value(2));

    MvcResult inboxResult = mvc.perform(get("/api/v1/notifications")
            .header(HttpHeaders.AUTHORIZATION, student1Token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.totalElements").value(2))
        .andReturn();
    JsonNode inboxItems = readJson(inboxResult).path("content").path("content");
    String pgvRecipientId = findRecipientIdByNotificationId(inboxItems, pgvNotificationId);
    assertThat(pgvRecipientId).isNotBlank();

    // 5. Student 1 marks PGV notification as read and PGV sees readCount = 1
    mvc.perform(patch("/api/v1/notifications/" + pgvRecipientId + "/read")
            .header(HttpHeaders.AUTHORIZATION, student1Token))
        .andExpect(status().isOk());

    mvc.perform(get("/api/v1/admin/notifications/" + pgvNotificationId)
            .header(HttpHeaders.AUTHORIZATION, staffToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.readCount").value(1));

    // 6. Student 1 marks all as read -> unreadCount = 0
    mvc.perform(patch("/api/v1/notifications/read-all")
            .header(HttpHeaders.AUTHORIZATION, student1Token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.markedCount").value(1));

    mvc.perform(get("/api/v1/notifications/unread-count")
            .header(HttpHeaders.AUTHORIZATION, student1Token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.unreadCount").value(0));

    // 7. Student 1 updates preferences (< 30m rejected, 45m accepted)
    mvc.perform(put("/api/v1/notifications/preferences")
            .header(HttpHeaders.AUTHORIZATION, student1Token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "inAppEnabled": true,
                  "zaloEnabled": true,
                  "reminderMinutesBefore": 15,
                  "receivePgvViaZalo": true,
                  "receiveLecturerViaZalo": true
                }
                """))
        .andExpect(status().isBadRequest());

    mvc.perform(put("/api/v1/notifications/preferences")
            .header(HttpHeaders.AUTHORIZATION, student1Token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "inAppEnabled": true,
                  "zaloEnabled": true,
                  "reminderMinutesBefore": 45,
                  "receivePgvViaZalo": true,
                  "receiveLecturerViaZalo": true
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.reminderMinutesBefore").value(45));

    // 8. Lecturer A revokes own announcement -> removed from Student 1 inbox
    mvc.perform(delete("/api/v1/lecturer/notifications/" + lecturerNotificationId)
            .header(HttpHeaders.AUTHORIZATION, lecturerAToken))
        .andExpect(status().isOk());

    mvc.perform(get("/api/v1/notifications")
            .header(HttpHeaders.AUTHORIZATION, student1Token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.totalElements").value(1));

    // 9. Phase 3: Online class reminder scans upcoming online session (25m away <= 30m/45m)
    int firstRunNotified = onlineClassReminderService.processUpcomingOnlineSessionReminders();
    assertThat(firstRunNotified).isEqualTo(3); // lecturerA + student1 + student2

    // 10. Re-running reminder scan must be strictly idempotent (0 duplicate reminders)
    int secondRunNotified = onlineClassReminderService.processUpcomingOnlineSessionReminders();
    assertThat(secondRunNotified).isZero();

    // 11. Student 1 inbox now has the CLASS_REMINDER referencing the online MeetingSession
    mvc.perform(get("/api/v1/notifications")
            .param("type", NotificationType.CLASS_REMINDER.name())
            .header(HttpHeaders.AUTHORIZATION, student1Token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.totalElements").value(1))
        .andExpect(jsonPath("$.content.content[0].referenceId").value(upcomingOnlineSession.getPublicId()));
  }

  private User saveUser(String email, String code, String fullName, AccountType type, String dept) {
    return userRepository.save(User.builder()
        .email(email)
        .code(code)
        .fullName(fullName)
        .accountType(type)
        .department(dept)
        .status(UserStatus.ACTIVE)
        .mustChangePassword(false)
        .build());
  }

  private String bearer(User user, Set<String> roles) {
    user.setRoles(roles);
    return "Bearer " + jwt.generateToken(user, UUID.randomUUID().toString()).accessToken();
  }

  private JsonNode readJson(MvcResult result) throws Exception {
    return objectMapper.readTree(result.getResponse().getContentAsString());
  }

  private String findRecipientIdByNotificationId(JsonNode items, String notificationId) {
    for (JsonNode item : items) {
      if (notificationId.equals(item.path("notificationId").asText())) {
        return item.path("recipientId").asText();
      }
    }
    return null;
  }
}
