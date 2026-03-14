package io.github.phunguy65.zms.usermanagement.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import io.github.phunguy65.zms.usermanagement.config.TestcontainersConfiguration;
import io.github.phunguy65.zms.usermanagement.infrastructure.security.FirebaseTokenVerifier;
import io.github.phunguy65.zms.usermanagement.presentation.request.LoginRequest;
import io.github.phunguy65.zms.usermanagement.presentation.request.RegisterRequest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
@Import(TestcontainersConfiguration.class)
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
        // Generate a unique username from the email prefix
        String username = email.replaceAll("[^a-zA-Z0-9_-]", "_")
                .substring(0, Math.min(30, email.indexOf('@')));
        return registerAndLogin(email, password, fullName, username);
    }

    /** Registers a user with an explicit username and returns their access token. */
    private String registerAndLogin(String email, String password, String fullName, String username)
            throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisterRequest(email, password, fullName, username))))
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

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.email").isNotEmpty())
                .andExpect(jsonPath("$.data.fullName").isNotEmpty())
                .andExpect(jsonPath("$.data.preferences.settings").isMap());
    }

    @Test
    void getMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
    }

    // ─── 11.2 GET /users/{id} ─────────────────────────────────────────────────

    @Test
    void getUserById_found_returns200() throws Exception {
        String token = registerAndLogin(
                "getbyid-" + System.nanoTime() + "@example.com", "password123", "Get By ID");

        // Get own ID from /me
        MvcResult meResult = mockMvc.perform(
                        get("/api/v1/me").header("Authorization", "Bearer " + token))
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

    // ─── 11.3 GET /users:search ───────────────────────────────────────────────

    @Test
    void searchUsers_defaultPagination_returns200WithScrollResponse() throws Exception {
        String token = registerAndLogin(
                "getusers-" + System.nanoTime() + "@example.com", "password123", "List User");

        mockMvc.perform(get("/api/v1/users:search").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void searchUsers_queryFilter_returnsMatchingUsers() throws Exception {
        String unique = "filter-" + System.nanoTime();
        String token = registerAndLogin(unique + "@example.com", "password123", "Filter User");

        mockMvc.perform(get("/api/v1/users:search")
                        .param("query", unique)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].email").value(unique + "@example.com"));
    }

    @Test
    void searchUsers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users:search")).andExpect(status().isUnauthorized());
    }

    // ─── 11.4 PATCH /users/me ─────────────────────────────────────────────────

    @Test
    void patchMe_partialUpdate_appliesChange() throws Exception {
        String token = registerAndLogin(
                "patch-" + System.nanoTime() + "@example.com", "password123", "Patch User");

        mockMvc.perform(patch("/api/v1/me")
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

        mockMvc.perform(patch("/api/v1/me")
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

        mockMvc.perform(patch("/api/v1/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.code").value("VALIDATION_ERROR"));
    }

    @Test
    void patchMe_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"X\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ─── 11.5 PATCH /users/me/preferences ────────────────────────────────────

    @Test
    void patchPreferences_partialUpdate_mergesFields() throws Exception {
        String token = registerAndLogin(
                "patchprefs-" + System.nanoTime() + "@example.com", "password123", "Prefs User");

        mockMvc.perform(patch("/api/v1/me/preferences")
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
        mockMvc.perform(patch("/api/v1/me/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\":\"blue\",\"customKey\":\"anything\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settings.theme").value("blue"))
                .andExpect(jsonPath("$.data.settings.customKey").value("anything"));
    }

    @Test
    void patchPreferences_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\":\"dark\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Username tests ───────────────────────────────────────────────────────

    @Test
    void getMe_responseIncludesUsername() throws Exception {
        String username = "getme_user_" + System.nanoTime() % 10000;
        String email = "getme-un-" + System.nanoTime() + "@example.com";
        String token = registerAndLogin(email, "password123", "Get Me Username", username);

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(username));
    }

    @Test
    void searchUsers_responseIncludesUsername() throws Exception {
        String username = "slice_user_" + System.nanoTime() % 10000;
        String email = "slice-un-" + System.nanoTime() + "@example.com";
        String token = registerAndLogin(email, "password123", "Slice Username", username);

        mockMvc.perform(get("/api/v1/users:search")
                        .param("query", email)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value(username));
    }

    @Test
    void patchMe_updateUsername_success() throws Exception {
        String originalUsername = "orig_user_" + System.nanoTime() % 10000;
        String newUsername = "new_user_" + System.nanoTime() % 10000;
        String token = registerAndLogin(
                "patch-un-" + System.nanoTime() + "@example.com",
                "password123",
                "Patch Username",
                originalUsername);

        mockMvc.perform(patch("/api/v1/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + newUsername + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(newUsername));
    }

    @Test
    void patchMe_duplicateUsername_returns409() throws Exception {
        String takenUsername = "taken_un_" + System.nanoTime() % 10000;
        // Register first user with the username
        registerAndLogin(
                "taken-un-" + System.nanoTime() + "@example.com",
                "password123",
                "Taken User",
                takenUsername);

        // Register second user and try to take the username
        String token = registerAndLogin(
                "second-un-" + System.nanoTime() + "@example.com",
                "password123",
                "Second User",
                "second_un_" + System.nanoTime() % 10000);

        mockMvc.perform(patch("/api/v1/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + takenUsername + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.code").value("USERNAME_ALREADY_EXISTS"));
    }

    // ─── 11.6 GET /users:search — cursor & pagination edge cases ─────────────

    @Test
    void searchUsers_invalidPageToken_returns400WithInvalidCursorCode() throws Exception {
        String token = registerAndLogin(
                "cursor-invalid-" + System.nanoTime() + "@example.com",
                "password123",
                "Cursor Test");

        mockMvc.perform(get("/api/v1/users:search")
                        .param("pageToken", "not!!!valid@base64token")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("INVALID_CURSOR"));
    }

    @Test
    void searchUsers_tamperedPageToken_returns400() throws Exception {
        String token = registerAndLogin(
                "cursor-tamper-" + System.nanoTime() + "@example.com",
                "password123",
                "Tamper Test");

        String fakeToken = java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        "1700000000000:00000000-0000-0000-0000-000000000001:badhmacsig".getBytes());

        mockMvc.perform(get("/api/v1/users:search")
                        .param("pageToken", fakeToken)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("fail"))
                .andExpect(jsonPath("$.data.code").value("INVALID_CURSOR"));
    }

    @Test
    void searchUsers_sizeOmitted_defaultsTo20() throws Exception {
        String token = registerAndLogin(
                "size-default-" + System.nanoTime() + "@example.com",
                "password123",
                "Size Default");

        mockMvc.perform(get("/api/v1/users:search").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void searchUsers_sizeGreaterThan100_clampsTo100() throws Exception {
        String token = registerAndLogin(
                "size-clamp-" + System.nanoTime() + "@example.com", "password123", "Size Clamp");

        mockMvc.perform(get("/api/v1/users:search")
                        .param("size", "500")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(100));
    }

    @Test
    void searchUsers_noMatchingQuery_returnsEmptyContentAndNullNextPageToken() throws Exception {
        String token = registerAndLogin(
                "empty-result-" + System.nanoTime() + "@example.com",
                "password123",
                "Empty Result");

        mockMvc.perform(get("/api/v1/users:search")
                        .param("query", "zzz_nonexistent_user_xyz_12345_abc")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.nextPageToken").doesNotExist());
    }

    @Test
    void searchUsers_queryMatchesUsername_returnsUser() throws Exception {
        String uniquePart = "uname_" + System.nanoTime() % 100000;
        String username = uniquePart;
        String email = "or-search-" + System.nanoTime() + "@example.com";
        String token = registerAndLogin(email, "password123", "OR Search User", username);

        // Search by username (not email) — verifies OR logic
        mockMvc.perform(get("/api/v1/users:search")
                        .param("query", uniquePart)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].username").value(username));
    }

    @Test
    void searchUsers_queryMatchesEmailOrUsername_returnsBothUsers() throws Exception {
        String sharedPart = "shared_" + System.nanoTime() % 100000;

        // user1: email contains sharedPart, username does not
        String token1 = registerAndLogin(
                sharedPart + "@example.com",
                "password123",
                "User One",
                "user1_" + System.nanoTime() % 100000);

        // user2: username contains sharedPart, email does not
        String token2 = registerAndLogin(
                "user2-" + System.nanoTime() + "@example.com",
                "password123",
                "User Two",
                sharedPart);

        // Query with sharedPart should match both users (OR semantics)
        mockMvc.perform(get("/api/v1/users:search")
                        .param("query", sharedPart)
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()")
                        .value(Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void searchUsers_multiPageTraversal_fetchesConsecutivePages() throws Exception {
        String prefix = "page_" + System.nanoTime() % 100000;

        // Register 5 users with a unique prefix in their email
        String firstToken = null;
        for (int i = 0; i < 5; i++) {
            String t = registerAndLogin(
                    prefix + "_u" + i + "@example.com",
                    "password123",
                    "Page User " + i,
                    prefix + "_u" + i);
            if (i == 0) firstToken = t;
        }

        // Fetch first page with size=2 — should get 2 items + nextPageToken
        MvcResult page1Result = mockMvc.perform(get("/api/v1/users:search")
                        .param("query", prefix)
                        .param("size", "2")
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.nextPageToken").isNotEmpty())
                .andReturn();

        String nextPageToken = objectMapper
                .readTree(page1Result.getResponse().getContentAsString())
                .at("/data/nextPageToken")
                .asText();

        // Fetch second page using the token — should get more items
        mockMvc.perform(get("/api/v1/users:search")
                        .param("query", prefix)
                        .param("size", "2")
                        .param("pageToken", nextPageToken)
                        .header("Authorization", "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()")
                        .value(Matchers.greaterThanOrEqualTo(1)));
    }
}
