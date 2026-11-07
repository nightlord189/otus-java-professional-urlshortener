package org.aburavov.urlshortener.controller;

import org.aburavov.urlshortener.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends BaseIntegrationTest {

    @Test
    void shouldRegisterUser() throws Exception {
        String email = randomEmail();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email.toUpperCase(), "password123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldNotRegisterSameEmailTwice() throws Exception {
        String email = randomEmail();
        registerAndLogin(email);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "another-password")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email is already registered"));
    }

    @Test
    void shouldRejectInvalidEmailAndShortPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("not-an-email", "password123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email is not valid"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(randomEmail(), "short")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Password must be 8-64 characters"));
    }

    @Test
    void shouldLoginWithCorrectPasswordOnly() throws Exception {
        String email = randomEmail();
        registerAndLogin(email);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "wrong-password")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("nobody@test.com", "password123")))
                .andExpect(status().isUnauthorized());
    }
}
