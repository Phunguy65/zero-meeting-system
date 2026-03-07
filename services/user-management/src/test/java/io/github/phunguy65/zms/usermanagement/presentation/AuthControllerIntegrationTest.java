package io.github.phunguy65.zms.usermanagement.presentation;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.phunguy65.zms.usermanagement.application.dto.LoginRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.LogoutRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.RefreshTokenRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    WebApplicationContext wac;

    ObjectMapper objectMapper = new ObjectMapper();

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void fullRegisterLoginRefreshLogoutFlow() throws Exception {
        // 1. Register
        var registerRequest =
                new RegisterRequest("integration@example.com", "password123", "Integration User");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.email").value("integration@example.com"));

        // 2. Login
        var loginRequest = new LoginRequest("integration@example.com", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        var loginBody = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginBody.at("/data/accessToken").asText();
        String refreshToken = loginBody.at("/data/refreshToken").asText();

        // 3. Refresh
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        var refreshBody = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String newRefreshToken = refreshBody.at("/data/refreshToken").asText();
        String newAccessToken = refreshBody.at("/data/accessToken").asText();

        // 4. Logout (requires valid access token)
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + newAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LogoutRequest(newRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        // 5. Reuse detection: old refresh token should be rejected
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegistrationReturns409() throws Exception {
        var request = new RegisterRequest("dup@example.com", "password123", "Dup User");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void invalidLoginReturns401() throws Exception {
        var request = new LoginRequest("nobody@example.com", "wrongpass");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void registerWithBlankFieldsReturns400WithViolations() throws Exception {
        // All fields blank — triggers @NotBlank on email, password, fullName
        var body = "{\"email\":\"\",\"password\":\"\",\"fullName\":\"\"}";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.errors", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.data.errors[0].field").isNotEmpty())
                .andExpect(jsonPath("$.data.errors[0].message").isNotEmpty())
                .andExpect(jsonPath("$.data.errors[0].code").isNotEmpty());
    }

    @Test
    void registerWithInvalidEmailReturns400WithEmailViolation() throws Exception {
        var body =
                "{\"email\":\"not-an-email\",\"password\":\"password123\",\"fullName\":\"Test User\"}";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.errors", hasSize(1)))
                .andExpect(jsonPath("$.data.errors[0].field").value("email"))
                .andExpect(jsonPath("$.data.errors[0].code").value("INVALID_FORMAT"));
    }

    @Test
    void registerWithShortPasswordReturns400WithTooShortViolation() throws Exception {
        var body =
                "{\"email\":\"valid@example.com\",\"password\":\"short\",\"fullName\":\"Test User\"}";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.errors", hasSize(1)))
                .andExpect(jsonPath("$.data.errors[0].field").value("password"))
                .andExpect(jsonPath("$.data.errors[0].code").value("TOO_SHORT"));
    }

    @Test
    void loginWithBlankFieldsReturns400WithViolations() throws Exception {
        var body = "{\"email\":\"\",\"password\":\"\"}";
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.errors", hasSize(greaterThan(0))));
    }
}
