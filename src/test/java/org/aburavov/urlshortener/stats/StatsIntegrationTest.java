package org.aburavov.urlshortener.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.aburavov.urlshortener.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;

class StatsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void statsCountClicksByDayAndReferer() throws Exception {
        String token = registerAndLogin("stats-1@example.com", "password123");
        String body = createLink(token, "{\"originalUrl\": \"https://example.com\"}");
        String code = JsonPath.read(body, "$.code");
        int linkId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/" + code).header(HttpHeaders.REFERER, "https://t.me/")).andExpect(status().isFound());
        mockMvc.perform(get("/" + code).header(HttpHeaders.REFERER, "https://t.me/")).andExpect(status().isFound());
        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        String today = LocalDate.now(ZoneOffset.UTC).toString();
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                mockMvc.perform(get("/api/v1/links/" + linkId + "/stats")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.linkId").value(linkId))
                        .andExpect(jsonPath("$.totalClicks").value(3))
                        .andExpect(jsonPath("$.clicksByDay.length()").value(1))
                        .andExpect(jsonPath("$.clicksByDay[0].date").value(today))
                        .andExpect(jsonPath("$.clicksByDay[0].clicks").value(3))
                        .andExpect(jsonPath("$.topReferers.length()").value(1))
                        .andExpect(jsonPath("$.topReferers[0].referer").value("https://t.me/"))
                        .andExpect(jsonPath("$.topReferers[0].clicks").value(2)));
    }

    @Test
    void clickStoresHashedIpOnly() throws Exception {
        String token = registerAndLogin("stats-2@example.com", "password123");
        String body = createLink(token, "{\"originalUrl\": \"https://example.com\"}");
        String code = JsonPath.read(body, "$.code");
        int linkId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<String> hashes = jdbcTemplate.queryForList(
                    "SELECT ip_hash FROM clicks WHERE link_id = ?", String.class, linkId);
            assertThat(hashes).hasSize(1);
            assertThat(hashes.get(0)).hasSize(64).doesNotContain("127.0.0.1");
        });
    }

    @Test
    void statsForOtherUsersLinkReturns404() throws Exception {
        String owner = registerAndLogin("stats-3@example.com", "password123");
        String other = registerAndLogin("stats-4@example.com", "password123");
        int linkId = JsonPath.read(createLink(owner, "{\"originalUrl\": \"https://example.com\"}"), "$.id");

        mockMvc.perform(get("/api/v1/links/" + linkId + "/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + other))
                .andExpect(status().isNotFound());
    }

    @Test
    void statsWithInvalidPeriodReturns400() throws Exception {
        String token = registerAndLogin("stats-5@example.com", "password123");
        int linkId = JsonPath.read(createLink(token, "{\"originalUrl\": \"https://example.com\"}"), "$.id");

        mockMvc.perform(get("/api/v1/links/" + linkId + "/stats")
                        .param("from", "2026-02-01T00:00:00Z")
                        .param("to", "2026-01-01T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/links/" + linkId + "/stats")
                        .param("from", "2020-01-01T00:00:00Z")
                        .param("to", "2026-01-01T00:00:00Z")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
