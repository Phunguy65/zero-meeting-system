package io.github.phunguy65.zms.usermanagement.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import io.github.phunguy65.zms.usermanagement.application.dto.LoginRequest;
import io.github.phunguy65.zms.usermanagement.application.dto.RegisterRequest;
import io.github.phunguy65.zms.usermanagement.infrastructure.security.FirebaseTokenVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class UserProfileIntegrationTest {

    @Autowired
    WebApplicationContext wac;

    @MockitoBean
    FirebaseTokenVerifier firebaseTokenVerifier;

    ObjectMapper objectMapper = new ObjectMapper();

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    /** Registers a user and returns their access token. */
    private String registerAndLogin(String email, String password, String fullName)
            throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, password, fullName))))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper
                .readTree(loginResult.getResponse().getContentAsString())
                .at("/data/accessToken")
                .asText();
    }

    // ─── 11.1 GET /users/me ───────────────────────────────────────────────────

    @Test
    void getMe_authenticated_returns200WithUserResponse() throws Exception {
        String token = registerAndLogin(
                "getme-" + System.nanoTime() + "@example.com", "password123", "Get Me User");

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.email").isNotEmpty())
                .andExpect(jsonPath("$.data.fullName").isNotEmpty())
                .andExpect(jsonPath("$.data.preferences.settings").isMap());
    }

    @Test
    void getMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
    }

    // ─── 11.2 GET /users/{id} ─────────────────────────────────────────────────

    @Test
    void getUserById_found_returns200() throws Exception {
        String token = registerAndLogin(
                "getbyid-" + System.nanoTime() + "@example.com", "password123", "Get By ID");

        // Get own ID from /me
        MvcResult meResult = mockMvc.perform(
                        get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String userId = objectMapper
                .readTree(meResult.getResponse().getContentAsString())
                .at("/data/id")
                .asText();

        mockMvc.perform(get("/api/v1/users/" + userId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.id").value(userId));
    }

    @Test
    void getUserById_notFound_returns404() throws Exception {
        String token = registerAndLogin(
                "getbyid-nf-" + System.nanoTime() + "@example.com", "password123", "Not Found");

        mockMvc.perform(get("/api/v1/users/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data.code").value("USER_NOT_FOUND"));
    }

    @Test
    void getUserById_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }

    // ─── 11.3 GET /users ──────────────────────────────────────────────────────

    @Test
    void getUsers_defaultPagination_returns200WithSlice() throws Exception {
        String token = registerAndLogin(
                "getusers-" + System.nanoTime() + "@example.com", "password123", "List User");

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void getUsers_emailFilter_returnsMatchingUsers() throws Exception {
        String unique = "filter-" + System.nanoTime();
        String token = registerAndLogin(unique + "@example.com", "password123", "Filter User");

        mockMvc.perform(get("/api/v1/users")
                        .param("email", unique)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].email").value(unique + "@example.com"));
    }

    @Test
    void getUsers_authProviderFilter_returnsMatchingUsers() throws Exception {
        String token = registerAndLogin(
                "provider-filter-" + System.nanoTime() + "@example.com",
                "password123",
                "Provider Filter");

        mockMvc.perform(get("/api/v1/users")
                        .param("authProvider", "EMAIL")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void getUsers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }

    // ─── 11.4 PATCH /users/me ─────────────────────────────────────────────────

    @Test
    void patchMe_partialUpdate_appliesChange() throws Exception {
        String token = registerAndLogin(
                "patch-" + System.nanoTime() + "@example.com", "password123", "Patch User");

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Updated Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.fullName").value("Updated Name"));
    }

    @Test
    void patchMe_emptyBody_isNoOp() throws Exception {
        String token = registerAndLogin(
                "patch-noop-" + System.nanoTime() + "@example.com", "password123", "NoOp User");

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("NoOp User"));
    }

    @Test
    void patchMe_blankFullName_returns400() throws Exception {
        String token = registerAndLogin(
                "patch-blank-" + System.nanoTime() + "@example.com", "password123", "Blank Test");

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    @Test
    void patchMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"X\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ─── 11.5 PATCH /users/me/preferences ────────────────────────────────────

    @Test
    void patchPreferences_partialUpdate_mergesFields() throws Exception {
        String token = registerAndLogin(
                "patchprefs-" + System.nanoTime() + "@example.com", "password123", "Prefs User");

        mockMvc.perform(patch("/api/v1/users/me/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\":\"dark\",\"fontSize\":14}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settings.theme").value("dark"))
                .andExpect(jsonPath("$.data.settings.fontSize").value(14));
    }

    @Test
    void patchPreferences_anyKeyAccepted() throws Exception {
        String token = registerAndLogin(
                "patchprefs-any-" + System.nanoTime() + "@example.com",
                "password123",
                "Any Key User");

        // Any key/value should be accepted — no validation errors
        mockMvc.perform(patch("/api/v1/users/me/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\":\"blue\",\"customKey\":\"anything\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settings.theme").value("blue"))
                .andExpect(jsonPath("$.data.settings.customKey").value("anything"));
    }

    @Test
    void patchPreferences_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\":\"dark\"}"))
                .andExpect(status().isUnauthorized());
    }
}
