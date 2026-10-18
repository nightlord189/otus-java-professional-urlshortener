package org.aburavov.urlshortener.admin;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import org.aburavov.urlshortener.AbstractIntegrationTest;
import org.aburavov.urlshortener.user.AdminProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AdminApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AdminProperties adminProperties;

    @Test
    void adminEndpointsAreForbiddenForUsers() throws Exception {
        String token = registerAndLogin("admin-test-1@example.com", "password123");

        mockMvc.perform(get("/api/v1/admin/links").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/stats").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/metrics").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminListsLinksFilteredByOwner() throws Exception {
        String user = registerAndLogin("admin-test-2@example.com", "password123");
        String admin = adminToken();
        int userId = currentUserId(user);
        createLink(user, "{\"originalUrl\": \"https://example.com/1\"}");
        createLink(user, "{\"originalUrl\": \"https://example.com/2\"}");

        mockMvc.perform(get("/api/v1/admin/links")
                        .param("ownerId", String.valueOf(userId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].ownerId").value(userId));

        mockMvc.perform(get("/api/v1/admin/links")
                        .param("ownerId", String.valueOf(userId))
                        .param("status", "DISABLED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void blockedLinkStopsRedirectingUntilUnblocked() throws Exception {
        String user = registerAndLogin("admin-test-3@example.com", "password123");
        String admin = adminToken();
        String body = createLink(user, "{\"originalUrl\": \"https://example.com\"}");
        String code = JsonPath.read(body, "$.code");
        int linkId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        mockMvc.perform(post("/api/v1/admin/links/" + linkId + "/block")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        mockMvc.perform(get("/" + code)).andExpect(status().isGone());

        mockMvc.perform(patch("/api/v1/links/" + linkId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "ACTIVE"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/admin/links/" + linkId + "/unblock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/" + code)).andExpect(status().isFound());
    }

    @Test
    void blockedUserCannotLogIn() throws Exception {
        String user = registerAndLogin("admin-test-4@example.com", "password123");
        String admin = adminToken();
        int userId = currentUserId(user);

        mockMvc.perform(post("/api/v1/admin/users/" + userId + "/block")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentialsJson("admin-test-4@example.com", "password123")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/users/" + userId + "/unblock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        login("admin-test-4@example.com", "password123");
    }

    @Test
    void adminCannotBeBlocked() throws Exception {
        String admin = adminToken();
        int adminId = currentUserId(admin);

        mockMvc.perform(post("/api/v1/admin/users/" + adminId + "/block")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                .andExpect(status().isConflict());
    }

    @Test
    void globalStatsCountUsersLinksAndClicks() throws Exception {
        String user = registerAndLogin("admin-test-5@example.com", "password123");
        String admin = adminToken();
        String code = JsonPath.read(createLink(user, "{\"originalUrl\": \"https://example.com\"}"), "$.code");

        mockMvc.perform(get("/" + code)).andExpect(status().isFound());
        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/admin/stats")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.totalUsers").value(greaterThanOrEqualTo(2)))
                        .andExpect(jsonPath("$.totalLinks").value(greaterThanOrEqualTo(1)))
                        .andExpect(jsonPath("$.activeLinks").value(greaterThanOrEqualTo(1)))
                        .andExpect(jsonPath("$.totalClicks").value(greaterThanOrEqualTo(2)))
                        .andExpect(jsonPath("$.clicksLast24h").value(greaterThanOrEqualTo(2)))
                        .andExpect(jsonPath("$.topLinks[*].code").value(hasItem(code))));
    }

    private String adminToken() throws Exception {
        return login(adminProperties.email(), adminProperties.password());
    }

    private int currentUserId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
