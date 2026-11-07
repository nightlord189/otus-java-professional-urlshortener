package org.aburavov.urlshortener.controller;

import com.jayway.jsonpath.JsonPath;
import org.aburavov.urlshortener.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatsControllerTest extends BaseIntegrationTest {

    @Test
    void shouldCountClicks() throws Exception {
        var token = registerAndLogin(randomEmail());
        var body = createLink(token, "{\"originalUrl\": \"https://example.com\"}");
        String code = JsonPath.read(body, "$.code");
        int linkId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/" + code).header("Referer", "https://t.me/")).andExpect(status().isFound());
        mockMvc.perform(get("/" + code).header("Referer", "https://t.me/")).andExpect(status().isFound());
        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        mockMvc.perform(get("/api/v1/links/" + linkId + "/stats").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkId").value(linkId))
                .andExpect(jsonPath("$.totalClicks").value(3))
                .andExpect(jsonPath("$.clicksByDay.length()").value(1))
                .andExpect(jsonPath("$.clicksByDay[0].date").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$.clicksByDay[0].clicks").value(3))
                .andExpect(jsonPath("$.topReferers.length()").value(1))
                .andExpect(jsonPath("$.topReferers[0].referer").value("https://t.me/"))
                .andExpect(jsonPath("$.topReferers[0].clicks").value(2));
    }

    @Test
    void shouldNotShowStatsOfOtherUsersLink() throws Exception {
        var owner = registerAndLogin(randomEmail());
        var other = registerAndLogin(randomEmail());
        int linkId = JsonPath.read(createLink(owner, "{\"originalUrl\": \"https://example.com\"}"), "$.id");

        mockMvc.perform(get("/api/v1/links/" + linkId + "/stats").header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/links/" + linkId + "/stats")
                        .param("from", "2026-02-01T00:00:00Z")
                        .param("to", "2026-01-01T00:00:00Z")
                        .header("Authorization", "Bearer " + owner))
                .andExpect(status().isBadRequest());
    }
}
