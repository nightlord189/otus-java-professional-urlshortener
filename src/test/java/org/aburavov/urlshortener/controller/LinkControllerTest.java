package org.aburavov.urlshortener.controller;

import com.jayway.jsonpath.JsonPath;
import org.aburavov.urlshortener.BaseIntegrationTest;
import org.aburavov.urlshortener.model.Link;
import org.aburavov.urlshortener.model.User;
import org.aburavov.urlshortener.model.UserRole;
import org.aburavov.urlshortener.repository.LinkRepository;
import org.aburavov.urlshortener.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LinkControllerTest extends BaseIntegrationTest {

    @Autowired
    private LinkRepository linkRepository;
    @Autowired
    private UserService userService;

    @Test
    void shouldCreateShortLink() throws Exception {
        String token = registerAndLogin(randomEmail());

        mockMvc.perform(post("/api/v1/links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\": \"https://example.com/some/long/page?x=1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").isNotEmpty())
                .andExpect(jsonPath("$.shortUrl").value(startsWith("http://localhost:8080/")))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/some/long/page?x=1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.expiresAt").value(nullValue()));
    }

    @Test
    void shouldCreateLinkWithAlias() throws Exception {
        String token = registerAndLogin(randomEmail());
        String alias = "Page" + System.nanoTime();

        mockMvc.perform(post("/api/v1/links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\": \"https://example.com\", \"alias\": \"" + alias + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(alias.toLowerCase()))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/" + alias.toLowerCase()));

        mockMvc.perform(post("/api/v1/links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\": \"https://example.org\", \"alias\": \"" + alias.toUpperCase() + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectBadUrlAndPastExpiry() throws Exception {
        String token = registerAndLogin(randomEmail());

        mockMvc.perform(post("/api/v1/links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\": \"not a url\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\": \"ftp://example.com/file\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/links")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"originalUrl\": \"https://example.com\", \"expiresAt\": \"2020-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Expiry must be in the future"));
    }

    @Test
    void shouldListOnlyOwnLinks() throws Exception {
        String serik = registerAndLogin(randomEmail());
        String tatyana = registerAndLogin(randomEmail());
        createLink(serik, "{\"originalUrl\": \"https://example.com/a1\"}");
        createLink(serik, "{\"originalUrl\": \"https://example.com/a2\"}");
        createLink(tatyana, "{\"originalUrl\": \"https://example.com/b1\"}");

        mockMvc.perform(get("/api/v1/links").header("Authorization", "Bearer " + serik))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].originalUrl").value("https://example.com/a2"))
                .andExpect(jsonPath("$.content[1].originalUrl").value("https://example.com/a1"));

        mockMvc.perform(get("/api/v1/links").param("size", "1000").header("Authorization", "Bearer " + serik))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotShowOtherUsersLink() throws Exception {
        String serik = registerAndLogin(randomEmail());
        String tatyana = registerAndLogin(randomEmail());
        int linkId = JsonPath.read(createLink(serik, "{\"originalUrl\": \"https://example.com\"}"), "$.id");

        mockMvc.perform(get("/api/v1/links/" + linkId).header("Authorization", "Bearer " + tatyana))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/links/" + linkId).header("Authorization", "Bearer " + serik))
                .andExpect(status().isOk());
    }

    @Test
    void shouldUpdateStatusAndExpiry() throws Exception {
        String token = registerAndLogin(randomEmail());
        String expiresAt = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS).toString();
        int linkId = JsonPath.read(createLink(token,
                "{\"originalUrl\": \"https://example.com\", \"expiresAt\": \"" + expiresAt + "\"}"), "$.id");

        mockMvc.perform(patch("/api/v1/links/" + linkId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DISABLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.expiresAt").value(expiresAt));

        mockMvc.perform(delete("/api/v1/links/" + linkId + "/expiry")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"))
                .andExpect(jsonPath("$.expiresAt").value(nullValue()));

        mockMvc.perform(patch("/api/v1/links/" + linkId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"BLOCKED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteLink() throws Exception {
        String token = registerAndLogin(randomEmail());
        int linkId = JsonPath.read(createLink(token, "{\"originalUrl\": \"https://example.com\"}"), "$.id");

        mockMvc.perform(delete("/api/v1/links/" + linkId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/links/" + linkId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Link not found"));

        mockMvc.perform(get("/api/v1/links").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0));
    }

    @Test
    void shouldRedirectToOriginalUrl() throws Exception {
        String token = registerAndLogin(randomEmail());
        String code = JsonPath.read(createLink(token, "{\"originalUrl\": \"https://example.com/target?x=1\"}"), "$.code");

        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/target?x=1"));

        mockMvc.perform(get("/" + code.toUpperCase()))
                .andExpect(status().isFound());
    }

    @Test
    void shouldReturn404ForUnknownCode() throws Exception {
        mockMvc.perform(get("/nope123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldStopRedirectingAfterDisableAndDelete() throws Exception {
        String token = registerAndLogin(randomEmail());
        String body = createLink(token, "{\"originalUrl\": \"https://example.com\"}");
        String code = JsonPath.read(body, "$.code");
        int linkId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        mockMvc.perform(patch("/api/v1/links/" + linkId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DISABLED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/" + code)).andExpect(status().isGone());

        mockMvc.perform(delete("/api/v1/links/" + linkId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/" + code)).andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn410ForExpiredLink() throws Exception {
        User owner = userService.create(randomEmail(), "password123", UserRole.USER);
        String code = "exp" + System.nanoTime();
        linkRepository.save(new Link(code, "https://example.com", owner, Instant.now().minusSeconds(60)));

        mockMvc.perform(get("/" + code))
                .andExpect(status().isGone());
    }
}
