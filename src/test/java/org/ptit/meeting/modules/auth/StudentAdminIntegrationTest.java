package org.ptit.meeting.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.ptit.meeting.modules.auth.constant.enums.AccountType;
import org.ptit.meeting.modules.auth.constant.enums.UserStatus;
import org.ptit.meeting.modules.auth.entity.Role;
import org.ptit.meeting.modules.auth.entity.User;
import org.ptit.meeting.modules.auth.repository.RoleRepository;
import org.ptit.meeting.modules.auth.repository.UserRepository;
import org.ptit.meeting.modules.auth.security.JwtTokenProvider;
import org.ptit.meeting.modules.auth.service.TokenService;
import org.ptit.meeting.modules.timetable.constant.enums.EnrollmentStatus;
import org.ptit.meeting.modules.timetable.entity.ClassEnrollment;
import org.ptit.meeting.modules.timetable.entity.ClassSection;
import org.ptit.meeting.modules.timetable.repository.ClassEnrollmentRepository;
import org.ptit.meeting.modules.timetable.repository.ClassSectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:student_admin_integration;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.liquibase.enabled=false",
    "spring.sql.init.mode=never",
    "application.livekit.enabled=false",
    "application.recording.enabled=false",
    "application.security.jwt.secret-key=student-admin-integration-test-key-0123456789"
})
@AutoConfigureMockMvc
@Transactional
class StudentAdminIntegrationTest {
  private static final String BASE = "/api/v1/admin/students";
  private static final String CREATE_BODY = """
      {"email":"Student@Example.org","code":"B22CN356","fullName":" First Student ",
       "department":" Engineering ","temporaryPassword":"initial-secret"}
      """;

  @Autowired private MockMvc mvc;
  @Autowired private JwtTokenProvider jwt;
  @Autowired private UserRepository users;
  @Autowired private RoleRepository roles;
  @Autowired private ClassSectionRepository sections;
  @Autowired private ClassEnrollmentRepository enrollments;
  @Autowired private PasswordEncoder passwords;
  @MockitoBean private TokenService tokenService;

  @Test
  void permitsStaffAndAdminStudentManagementButRejectsOtherRoles() throws Exception {
    mvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/admin/student-structure/options"))
        .andExpect(status().isUnauthorized());
    for (String role : Set.of("ROLE_STUDENT", "ROLE_LECTURER")) {
      mvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, bearer(role)))
          .andExpect(status().isForbidden());
      mvc.perform(patch(BASE + "/bulk-status").header(HttpHeaders.AUTHORIZATION, bearer(role))
              .contentType(MediaType.APPLICATION_JSON).content("""
                  {"ids":[1],"status":"LOCKED"}
                  """))
          .andExpect(status().isForbidden());
      mvc.perform(get("/api/v1/admin/student-structure/options")
              .header(HttpHeaders.AUTHORIZATION, bearer(role)))
          .andExpect(status().isForbidden());
    }
    mvc.perform(get(BASE + "/summary").header(HttpHeaders.AUTHORIZATION, bearer("ROLE_ADMIN")))
        .andExpect(status().isOk());
    User student = account("managed", AccountType.STUDENT, null, UserStatus.ACTIVE, "Student");
    String staff = bearer("ROLE_STAFF");
    mvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, staff))
        .andExpect(status().isOk());
    mvc.perform(get(BASE + "/{id}", student.getId()).header(HttpHeaders.AUTHORIZATION, staff))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.id").value(student.getId().intValue()));
    mvc.perform(get(BASE + "/export").param("format", "CSV")
            .header(HttpHeaders.AUTHORIZATION, staff))
        .andExpect(status().isOk());
    for (String path : Set.of("options", "tree")) {
      mvc.perform(get("/api/v1/admin/student-structure/" + path)
              .header(HttpHeaders.AUTHORIZATION, staff))
          .andExpect(status().isOk());
    }
    mvc.perform(patch(BASE + "/{id}/status", student.getId())
            .header(HttpHeaders.AUTHORIZATION, staff)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"status":"LOCKED"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.status").value("LOCKED"));
    mvc.perform(post(BASE + "/{id}/reset-password", student.getId())
            .header(HttpHeaders.AUTHORIZATION, staff)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"temporaryPassword":"replacement-secret"}
                """))
        .andExpect(status().isOk());
    mvc.perform(delete(BASE + "/{id}", student.getId()).header(HttpHeaders.AUTHORIZATION, staff))
        .andExpect(status().isOk());
    assertThat(users.findById(student.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.INACTIVE);
    mvc.perform(post("/api/v1/admin/student-structure/campuses")
            .header(HttpHeaders.AUTHORIZATION, staff)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"code":"TEST","name":"Test campus"}
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content.code").value("TEST"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ROLE_ADMIN", "ROLE_STAFF"})
  void createsUpdatesAndResetsOnlyTheLocalStudentAccount(String role) throws Exception {
    roles.saveAndFlush(Role.builder().code("ROLE_STUDENT").name("Student").build());
    String admin = bearer(role);
    mvc.perform(post(BASE).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.content.code").value("B22DCCN356"))
        .andExpect(jsonPath("$.content.fullName").value("First Student"))
        .andExpect(jsonPath("$.content.department").value("Engineering"))
        .andExpect(jsonPath("$.content.status").value("ACTIVE"))
        .andExpect(jsonPath("$.content.mustChangePassword").value(true))
        .andExpect(jsonPath("$.content.passwordHash").doesNotExist())
        .andExpect(jsonPath("$.content.temporaryPassword").doesNotExist());
    User student = users.findByEmail("student@example.org").orElseThrow();
    assertThat(roles.findRoleCodesByUserId(student.getId())).containsExactly("ROLE_STUDENT");
    assertThat(passwords.matches("initial-secret", student.getPasswordHash())).isTrue();

    mvc.perform(put(BASE + "/{id}", student.getId()).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"student@example.org","code":"B22DCCN356","fullName":"Updated Student"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.fullName").value("Updated Student"));
    assertThat(student.getDepartment()).isNull();

    student.setMustChangePassword(false);
    users.flush();
    mvc.perform(post(BASE + "/{id}/reset-password", student.getId()).header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"temporaryPassword":"replacement-secret"}
                """))
        .andExpect(status().isOk());
    assertThat(passwords.matches("replacement-secret", student.getPasswordHash())).isTrue();
    assertThat(student.isMustChangePassword()).isTrue();
  }

  @Test
  void rejectsLegacyShortCodeCollisionsAcrossAccountTypes() throws Exception {
    roles.saveAndFlush(Role.builder().code("ROLE_STUDENT").name("Student").build());
    users.saveAndFlush(User.builder().accountType(AccountType.STAFF)
        .code("B22CN356").email("staff@example.org").fullName("Staff").build());
    mvc.perform(post(BASE).header(HttpHeaders.AUTHORIZATION, bearer("ROLE_ADMIN"))
            .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.strCode").value("error.student-admin.code-exists"));
    assertThat(users.findByEmail("student@example.org")).isEmpty();
  }

  @Test
  void rejectsPasswordThatExceedsBcryptByteLimit() throws Exception {
    User student = account("student", AccountType.STUDENT, null, UserStatus.ACTIVE, "Student");
    mvc.perform(post(BASE + "/{id}/reset-password", student.getId())
            .header(HttpHeaders.AUTHORIZATION, bearer("ROLE_ADMIN"))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"temporaryPassword\":\"" + "ế".repeat(25) + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.strCode").value("validation.student-admin.password.size"));
    assertThat(student.getPasswordHash()).isNull();
  }

  @Test
  void queriesAndExportsOnlyMatchingStudentRowsWithLiteralKeywordAndStablePaging() throws Exception {
    User first = account("first", AccountType.STUDENT, " Engineering ", UserStatus.ACTIVE, "=Tést_%");
    User second = account("second", AccountType.STUDENT, "Engineering", UserStatus.ACTIVE, "=Tést_%");
    account("third", AccountType.STUDENT, "Engineering", UserStatus.ACTIVE, "TéstAB");
    account("locked", AccountType.STUDENT, "Engineering", UserStatus.LOCKED, "=Tést_%");
    account("staff", AccountType.STAFF, "Staff department", UserStatus.ACTIVE, "=Tést_%");
    String admin = bearer("ROLE_ADMIN");

    mvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, admin).param("keyword", "_%")
            .param("department", "engineering").param("status", "ACTIVE")
            .param("sortBy", "fullName").param("direction", "ASC").param("size", "1").param("page", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.totalElements").value(2))
        .andExpect(jsonPath("$.content.content[0].id").value(second.getId().intValue()));
    mvc.perform(get(BASE + "/filters").header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.departments.length()").value(1))
        .andExpect(jsonPath("$.content.departments[0]").value("Engineering"));
    mvc.perform(get(BASE + "/summary").header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.total").value(4))
        .andExpect(jsonPath("$.content.active").value(3))
        .andExpect(jsonPath("$.content.locked").value(1));
    mvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, admin).param("sortBy", "passwordHash"))
        .andExpect(status().isBadRequest());
    mvc.perform(get(BASE).header(HttpHeaders.AUTHORIZATION, admin).param("size", "101"))
        .andExpect(status().isBadRequest());

    byte[] csv = mvc.perform(get(BASE + "/export").header(HttpHeaders.AUTHORIZATION, admin)
            .param("format", "CSV").param("keyword", "_%").param("status", "ACTIVE").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
        .andReturn().getResponse().getContentAsByteArray();
    assertThat(new String(csv, StandardCharsets.UTF_8)).startsWith("\uFEFF")
        .contains(first.getEmail(), second.getEmail(), "\"'=Tést_%\"")
        .doesNotContain("third@example.org", "locked@example.org", "staff@example.org", "passwordHash");

    byte[] xlsx = mvc.perform(get(BASE + "/export").header(HttpHeaders.AUTHORIZATION, admin)
            .param("keyword", "_%").param("status", "ACTIVE"))
        .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
      var sheet = workbook.getSheetAt(0);
      assertThat(sheet.getLastRowNum()).isEqualTo(2);
      var name = sheet.getRow(1).getCell(2);
      assertThat(name.getCellType()).isEqualTo(CellType.STRING);
      assertThat(name.getStringCellValue()).isEqualTo("=Tést_%");
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"ROLE_ADMIN", "ROLE_STAFF"})
  void rejectsMixedBulkTargetsWithoutChangingAnyStatus(String role) throws Exception {
    User student = account("student", AccountType.STUDENT, null, UserStatus.ACTIVE, "Student");
    User staff = account("staff", AccountType.STAFF, null, UserStatus.ACTIVE, "Staff");
    String admin = bearer(role);
    mvc.perform(patch(BASE + "/bulk-status").header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"ids":[%d,%d],"status":"LOCKED"}
                """.formatted(student.getId(), staff.getId())))
        .andExpect(status().isNotFound());
    assertThat(student.getStatus()).isEqualTo(UserStatus.ACTIVE);
    assertThat(staff.getStatus()).isEqualTo(UserStatus.ACTIVE);
    mvc.perform(get(BASE + "/{id}", staff.getId()).header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isNotFound());
  }

  @ParameterizedTest
  @ValueSource(strings = {"ROLE_ADMIN", "ROLE_STAFF"})
  void archivesReferencedStudentAndReadsOnlyClassEnrollmentsFromTimetable(String role) throws Exception {
    User student = account("student", AccountType.STUDENT, null, UserStatus.ACTIVE, "Student");
    ClassSection section = sections.saveAndFlush(ClassSection.builder()
        .semesterId(101L).subjectId(201L).classCode("TEST-01").build());
    ClassEnrollment enrollment = enrollments.saveAndFlush(ClassEnrollment.builder()
        .classSectionId(section.getId()).userId(student.getId()).status(EnrollmentStatus.DROPPED).build());
    enrollments.saveAndFlush(ClassEnrollment.builder()
        .roomId(UUID.randomUUID().toString()).userId(student.getId()).build());
    String admin = bearer(role);
    mvc.perform(patch(BASE + "/bulk-status").header(HttpHeaders.AUTHORIZATION, admin)
            .contentType(MediaType.APPLICATION_JSON).content("""
                {"ids":[%d,%d],"status":"LOCKED"}
                """.formatted(student.getId(), student.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.matched").value(1))
        .andExpect(jsonPath("$.content.changed").value(1));
    mvc.perform(delete(BASE + "/{id}", student.getId()).header(HttpHeaders.AUTHORIZATION, admin))
        .andExpect(status().isOk());
    assertThat(users.findById(student.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.INACTIVE);
    assertThat(enrollments.findById(enrollment.getId())).isPresent();
    mvc.perform(get(BASE + "/{id}/enrollments", student.getId()).header(HttpHeaders.AUTHORIZATION, admin)
            .param("semesterId", "101").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.totalElements").value(1))
        .andExpect(jsonPath("$.content.content[0].classCode").value("TEST-01"))
        .andExpect(jsonPath("$.content.content[0].status").value("DROPPED"));
    mvc.perform(get(BASE + "/{id}/enrollments", student.getId()).header(HttpHeaders.AUTHORIZATION, admin)
            .param("semesterId", "102"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.totalElements").value(0));
  }

  private User account(String emailPrefix, AccountType type, String department, UserStatus status, String name) {
    return users.saveAndFlush(User.builder().email(emailPrefix + "@example.org").code(emailPrefix)
        .accountType(type).department(department).status(status).fullName(name).build());
  }

  private String bearer(String role) {
    User user = User.builder().id(1L).email("jwt@example.org").roles(Set.of(role)).build();
    return "Bearer " + jwt.generateToken(user, UUID.randomUUID().toString()).accessToken();
  }
}
