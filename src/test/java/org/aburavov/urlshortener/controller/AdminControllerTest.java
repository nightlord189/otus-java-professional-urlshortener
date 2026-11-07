package org.aburavov.urlshortener.controller;

import com.jayway.jsonpath.JsonPath;
import org.aburavov.urlshortener.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest extends BaseIntegrationTest {

    @Test
    void shouldForbidAdminApiForUsers() throws Exception {
        String token = registerAndLogin(randomEmail());

        mockMvc.perform(get("/api/v1/admin/links").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldListLinksByOwner() throws Exception {
        String user = registerAndLogin(randomEmail());
        String admin = adminLogin();
        int userId = currentUserId(user);
        createLink(user, "{\"originalUrl\": \"https://example.com/1\"}");
        createLink(user, "{\"originalUrl\": \"https://example.com/2\"}");

        mockMvc.perform(get("/api/v1/admin/links")
                        .param("ownerId", String.valueOf(userId))
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].ownerId").value(userId));

        mockMvc.perform(get("/api/v1/admin/links")
                        .param("ownerId", String.valueOf(userId))
                        .param("status", "DISABLED")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void shouldBlockAndUnblockLink() throws Exception {
        String user = registerAndLogin(randomEmail());
        String admin = adminLogin();
        String body = createLink(user, "{\"originalUrl\": \"https://example.com\"}");
        String code = JsonPath.read(body, "$.code");
        int linkId = JsonPath.read(body, "$.id");

        mockMvc.perform(post("/api/v1/admin/links/" + linkId + "/block").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));
        mockMvc.perform(get("/" + code)).andExpect(status().isGone());

        mockMvc.perform(patch("/api/v1/links/" + linkId)
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/admin/links/" + linkId + "/unblock").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(get("/" + code)).andExpect(status().isFound());
    }

    @Test
    void shouldBlockUser() throws Exception {
        String email = randomEmail();
        String user = registerAndLogin(email);
        String admin = adminLogin();
        int userId = currentUserId(user);

        mockMvc.perform(post("/api/v1/admin/users/" + userId + "/block").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "password123")))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + user))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/admin/users/" + userId + "/unblock").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        login(email, "password123");

        mockMvc.perform(post("/api/v1/admin/users/" + currentUserId(admin) + "/block")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldShowGlobalStats() throws Exception {
        String user = registerAndLogin(randomEmail());
        String admin = adminLogin();
        String code = JsonPath.read(createLink(user, "{\"originalUrl\": \"https://example.com\"}"), "$.code");

        mockMvc.perform(get("/" + code)).andExpect(status().isFound());
        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        mockMvc.perform(get("/api/v1/admin/stats").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.totalLinks").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalClicks").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.clicksLast24h").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.topLinks[*].code").value(hasItem(code)));
    }

    private int currentUserId(String token) throws Exception {
        String body = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}
