package org.aburavov.urlshortener.redirect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.aburavov.urlshortener.AbstractIntegrationTest;
import org.aburavov.urlshortener.config.CacheConfig;
import org.aburavov.urlshortener.link.Link;
import org.aburavov.urlshortener.link.LinkRepository;
import org.aburavov.urlshortener.user.User;
import org.aburavov.urlshortener.user.UserRole;
import org.aburavov.urlshortener.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class RedirectIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private LinkRepository linkRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private CacheManager cacheManager;
    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void redirectsToOriginalUrl() throws Exception {
        String token = registerAndLogin("redirect-1@example.com", "password123");
        String code = JsonPath.read(createLink(token, "{\"originalUrl\": \"https://example.com/target?x=1\"}"), "$.code");

        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string(HttpHeaders.LOCATION, "https://example.com/target?x=1"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"));
    }

    @Test
    void unknownCodeReturns404() throws Exception {
        mockMvc.perform(get("/nope123"))
                .andExpect(status().isNotFound());
    }

    @Test
    void redirectIsCached() throws Exception {
        String token = registerAndLogin("redirect-2@example.com", "password123");
        String code = JsonPath.read(createLink(token, "{\"originalUrl\": \"https://example.com\"}"), "$.code");

        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        assertThat(cacheManager.getCache(CacheConfig.LINKS_CACHE).get(code)).isNotNull();
    }

    @Test
    void disabledLinkReturns410EvenWhenCached() throws Exception {
        String token = registerAndLogin("redirect-3@example.com", "password123");
        String body = createLink(token, "{\"originalUrl\": \"https://example.com\"}");
        String code = JsonPath.read(body, "$.code");
        int linkId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        mockMvc.perform(patch("/api/v1/links/" + linkId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "DISABLED"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/" + code)).andExpect(status().isGone());
    }

    @Test
    void deletedLinkReturns404() throws Exception {
        String token = registerAndLogin("redirect-4@example.com", "password123");
        String body = createLink(token, "{\"originalUrl\": \"https://example.com\"}");
        String code = JsonPath.read(body, "$.code");
        int linkId = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        mockMvc.perform(delete("/api/v1/links/" + linkId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/" + code)).andExpect(status().isNotFound());
    }

    @Test
    void redirectsAreCounted() throws Exception {
        String token = registerAndLogin("redirect-6@example.com", "password123");
        String code = JsonPath.read(createLink(token, "{\"originalUrl\": \"https://example.com\"}"), "$.code");
        double before = meterRegistry.counter("shortener.redirects", "outcome", "found").count();

        mockMvc.perform(get("/" + code)).andExpect(status().isFound());
        mockMvc.perform(get("/" + code)).andExpect(status().isFound());

        assertThat(meterRegistry.counter("shortener.redirects", "outcome", "found").count()).isEqualTo(before + 2);
    }

    @Test
    void expiredLinkReturns410() throws Exception {
        User owner = userService.create("redirect-5@example.com", "password123", UserRole.USER);
        linkRepository.save(new Link("expired1", "https://example.com", owner, Instant.now().minusSeconds(60)));

        mockMvc.perform(get("/expired1"))
                .andExpect(status().isGone());
    }
}
