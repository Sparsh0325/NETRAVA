package com.intellishare.controller;

import com.intellishare.model.User;
import com.intellishare.repository.UserRepository;
import com.intellishare.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AuthControllerTest.AdminTestController.class)
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtTokenService tokens;

    @BeforeEach
    void clearUsers() { users.deleteAll(); }

    @Test
    void registerStoresBcryptAndNeverReturnsHashOrPrivilegedRole() throws Exception {
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("analyst@example.test", "badge-1", ",\"role\":\"Administrator\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("Read-Only"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        User saved = users.findByEmailIgnoreCase("analyst@example.test").orElseThrow();
        assertNotEquals("CorrectHorseBattery1", saved.getPasswordHash());
        assertTrue(passwordEncoder.matches("CorrectHorseBattery1", saved.getPasswordHash()));
        assertNotNull(mapper.readTree(response).get("token").asText());
    }

    @Test
    void duplicateEmailOrBadgeReturnsConflict() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("same@example.test", "badge-2", "")))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("same@example.test", "badge-3", "")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("other@example.test", "badge-2", "")))
                .andExpect(status().isConflict());
    }

    @Test
    void loginValidatesCredentialsAndMeRequiresValidToken() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("login@example.test", "badge-4", "")))
                .andExpect(status().isCreated());
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@example.test\",\"password\":\"CorrectHorseBattery1\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode body = mapper.readTree(login);
        String token = body.get("token").asText();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("login@example.test"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@example.test\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registrationValidatesInputAndAdminRouteRejectsOfficer() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"bad\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("officer@example.test", "badge-5", "")))
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(response).get("token").asText();
        mvc.perform(get("/api/test/admin").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void expiredJwtIsRejected() throws Exception {
        String secret = "VGhpc0lzQVRlc3RPbmx5U2VjcmV0S2V5Rm9ySldUSFNpZ25pbmc=";
        JwtTokenService shortLived = new JwtTokenService(secret, Duration.ofMillis(1));
        User user = new User("Test", "test@example.test", "hash", "badge", "agency", null, null);
        String token = shortLived.issue(user);
        Thread.sleep(10);
        assertThrows(JwtException.class, () -> shortLived.parse(token));
    }

    private String registerJson(String email, String badge, String extra) {
        return "{\"name\":\"Test Officer\",\"email\":\"" + email + "\",\"password\":\"CorrectHorseBattery1\","
                + "\"badgeNumber\":\"" + badge + "\",\"agency\":\"Test Agency\"" + extra + "}";
    }

    @RestController
    @RequestMapping("/api/test/admin")
    @PreAuthorize("hasRole('Administrator')")
    static class AdminTestController {
        @GetMapping
        String adminOnly() { return "ok"; }
    }
}
