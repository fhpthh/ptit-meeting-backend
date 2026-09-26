package org.ptit.meeting;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MeetingApplicationTests {

  @Autowired
  private MockMvc mockMvc;

  @Test
  @Order(1)
  void contextLoads() {
  }

  @Test
  @Order(2)
  @DisplayName("Test 1: Lấy URL đăng nhập Microsoft Outlook SSO")
  void testGetOutlookAuthorizeUrl() throws Exception {
    mockMvc.perform(get("/api/v1/auth/outlook/authorize-url"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.authorizeUrl").isNotEmpty());
  }

  @Test
  @Order(3)
  @DisplayName("Test 2: Sinh viên đăng nhập lần đầu - bắt buộc đổi mật khẩu (mustChangePassword = true)")
  void testLoginStudentMustChangePassword() throws Exception {
    String loginBody = """
        {
          "username": "B22DCCN001",
          "password": "Ptit@123"
        }
        """;

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginBody))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.mustChangePassword").value(true))
        .andExpect(jsonPath("$.data.user.email").value("b22dccn001@stu.ptit.edu.vn"))
        .andExpect(jsonPath("$.data.user.code").value("B22DCCN001"));
  }

  @Test
  @Order(4)
  @DisplayName("Test 3: Giảng viên đăng nhập thành công - cấp Access Token & Refresh Token (mustChangePassword = false)")
  void testLoginLecturerSuccess() throws Exception {
    String loginBody = """
        {
          "username": "GV001",
          "password": "Ptit@123"
        }
        """;

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginBody))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.mustChangePassword").value(false))
        .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.data.user.roles[0]").value("ROLE_LECTURER"));
  }

  @Test
  @Order(5)
  @DisplayName("Test 4: Đăng nhập sai mật khẩu - trả về lỗi 401 Unauthorized và ghi nhận brute-force vào Redis")
  void testLoginWrongPassword() throws Exception {
    String loginBody = """
        {
          "username": "GV001",
          "password": "WrongPassword@123"
        }
        """;

    mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginBody))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.apiError.code").value("AUTH_003"));
  }

  @Test
  @Order(6)
  @DisplayName("Test 5: Lấy profile người dùng hiện tại qua GET /api/v1/auth/me với Bearer Token")
  void testGetMeAfterLogin() throws Exception {
    String loginBody = """
        {
          "username": "GV001",
          "password": "Ptit@123"
        }
        """;

    String responseString = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginBody))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    // Extract access token
    com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseString);
    String token = rootNode.path("data").path("accessToken").asText();

    mockMvc.perform(get("/api/v1/auth/me")
            .header("Authorization", "Bearer " + token))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.code").value("GV001"))
        .andExpect(jsonPath("$.data.email").value("gv001@ptit.edu.vn"))
        .andExpect(jsonPath("$.data.roles[0]").value("ROLE_LECTURER"));
  }

  @Test
  @Order(7)
  @DisplayName("Test 6: Làm mới phiên đăng nhập qua POST /api/v1/auth/refresh với Refresh Token")
  void testRefreshTokenSuccess() throws Exception {
    String loginBody = """
        {
          "username": "GV001",
          "password": "Ptit@123"
        }
        """;

    String responseString = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginBody))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseString);
    String refreshToken = rootNode.path("data").path("refreshToken").asText();

    String refreshBody = String.format("{\"refreshToken\": \"%s\"}", refreshToken);

    mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(refreshBody))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
  }

  @Test
  @Order(8)
  @DisplayName("Test 7: Đăng xuất an toàn: Thu hồi Refresh Token & Blacklist Access Token")
  void testLogoutAndTokenBlacklisted() throws Exception {
    String loginBody = """
        {
          "username": "GV001",
          "password": "Ptit@123"
        }
        """;

    String responseString = mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(loginBody))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseString);
    String accessToken = rootNode.path("data").path("accessToken").asText();
    String refreshToken = rootNode.path("data").path("refreshToken").asText();

    // 1. Thực hiện Logout
    String logoutBody = String.format("{\"refreshToken\": \"%s\"}", refreshToken);
    mockMvc.perform(post("/api/v1/auth/logout")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(logoutBody))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200));

    // 2. Kiểm tra Access Token cũ đã bị Blacklist chặn
    mockMvc.perform(get("/api/v1/auth/me")
            .header("Authorization", "Bearer " + accessToken))
        .andDo(print())
        .andExpect(status().isUnauthorized());

    // 3. Kiểm tra Refresh Token cũ đã bị thu hồi khỏi Redis
    String refreshBody = String.format("{\"refreshToken\": \"%s\"}", refreshToken);
    mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(refreshBody))
        .andDo(print())
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.apiError.code").value("AUTH_007"));
  }
}
