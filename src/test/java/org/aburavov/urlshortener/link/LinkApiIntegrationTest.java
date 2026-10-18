package org.aburavov.urlshortener.link;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.aburavov.urlshortener.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class LinkApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createReturnsShortLink() throws Exception {
        String token = registerAndLogin("link-owner-1@example.com", "password123");

        mockMvc.perform(post("/api/v1/links")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl": "https://example.com/some/long/page?x=1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").value(matchesPattern("[0-9A-Za-z]{7}")))
                .andExpect(jsonPath("$.shortUrl").value(startsWith("http://localhost:8080/")))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/some/long/page?x=1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.expiresAt").value(nullValue()));
    }

    @Test
    void createWithAliasUsesIt() throws Exception {
        String token = registerAndLogin("link-owner-2@example.com", "password123");

        mockMvc.perform(post("/api/v1/links")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl": "https://example.com", "alias": "my-page"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("my-page"))
                .andExpect(jsonPath("$.shortUrl").value(endsWith("/my-page")));

        mockMvc.perform(post("/api/v1/links")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl": "https://example.org", "alias": "my-page"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void createWithInvalidUrlReturns400() throws Exception {
        String token = registerAndLogin("link-owner-3@example.com", "password123");

        mockMvc.perform(post("/api/v1/links")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl": "not a url"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithPastExpiryReturns400() throws Exception {
        String token = registerAndLogin("link-owner-4@example.com", "password123");

        mockMvc.perform(post("/api/v1/links")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl": "https://example.com", "expiresAt": "2020-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Expiry must be in the future"));
    }

    @Test
    void createWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"originalUrl": "https://example.com"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listReturnsOnlyOwnLinks() throws Exception {
        String alice = registerAndLogin("link-owner-5@example.com", "password123");
        String bob = registerAndLogin("link-owner-6@example.com", "password123");
        createLink(alice, "{\"originalUrl\": \"https://example.com/a1\"}");
        createLink(alice, "{\"originalUrl\": \"https://example.com/a2\"}");
        createLink(bob, "{\"originalUrl\": \"https://example.com/b1\"}");

        mockMvc.perform(get("/api/v1/links")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].originalUrl").value("https://example.com/a2"))
                .andExpect(jsonPath("$.content[1].originalUrl").value("https://example.com/a1"));
    }

    @Test
    void getOtherUsersLinkReturns404() throws Exception {
        String alice = registerAndLogin("link-owner-7@example.com", "password123");
        String bob = registerAndLogin("link-owner-8@example.com", "password123");
        int linkId = JsonPath.read(createLink(alice, "{\"originalUrl\": \"https://example.com\"}"), "$.id");

        mockMvc.perform(get("/api/v1/links/" + linkId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + bob))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/links/" + linkId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + alice))
                .andExpect(status().isOk());
    }

    @Test
    void patchUpdatesStatusAndExpiry() throws Exception {
        String token = registerAndLogin("link-owner-9@example.com", "password123");
        String expiresAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        int linkId = JsonPath.read(createLink(token,
                "{\"originalUrl\": \"https://example.com\", \"expiresAt\": \"" + expiresAt + "\"}"), "$.id");

        mockMvc.perform(patch("/api/v1/links/" + linkId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.expiresAt").value(expiresAt));

        String newExpiresAt = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        mockMvc.perform(patch("/api/v1/links/" + linkId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expiresAt\": \"" + newExpiresAt + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.expiresAt").value(newExpiresAt));

        mockMvc.perform(delete("/api/v1/links/" + linkId + "/expiry")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.expiresAt").value(nullValue()));

        mockMvc.perform(patch("/api/v1/links/" + linkId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "BLOCKED"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteHidesLink() throws Exception {
        String token = registerAndLogin("link-owner-10@example.com", "password123");
        int linkId = JsonPath.read(createLink(token, "{\"originalUrl\": \"https://example.com\"}"), "$.id");

        mockMvc.perform(delete("/api/v1/links/" + linkId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/links/" + linkId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/links")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
