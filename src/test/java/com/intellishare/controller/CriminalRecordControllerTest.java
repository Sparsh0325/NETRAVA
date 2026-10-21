package com.intellishare.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellishare.repository.CriminalRecordRepository;
import com.intellishare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CriminalRecordControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired CriminalRecordRepository records;

    @BeforeEach void clearData() { records.deleteAll(); }

    @Test
    void createsSearchesAndPagesPersistentRecords() throws Exception {
        String token = register("registry-officer-" + java.util.UUID.randomUUID() + "@example.test");
        String created = mvc.perform(post("/api/criminal-records").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(recordJson("Registry Test", "Fraud")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.recordNumber").exists())
                .andExpect(jsonPath("$.syntheticDemo").value(false)).andReturn().getResponse().getContentAsString();
        String number = mapper.readTree(created).get("recordNumber").asText();
        assertEquals(1, records.count());
        mvc.perform(get("/api/criminal-records").param("q", "Registry Test").param("page", "0").param("size", "1")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].recordNumber").value(number));
    }

    @Test
    void validatesInputAndProtectsRecordsByRole() throws Exception {
        mvc.perform(get("/api/criminal-records")).andExpect(status().isUnauthorized());
        String token = register("registry-officer-" + java.util.UUID.randomUUID() + "@example.test");
        mvc.perform(post("/api/criminal-records").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"status\":\"Wanted\",\"riskLevel\":\"High\"}"))
                .andExpect(status().isBadRequest());
        String created = mvc.perform(post("/api/criminal-records").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(recordJson("Delete Guard", "Theft")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String number = mapper.readTree(created).get("recordNumber").asText();
        mvc.perform(delete("/api/criminal-records/{number}", number).header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicRegistrationCannotCreateRecords() throws Exception {
        String email = "registry-readonly-" + java.util.UUID.randomUUID() + "@example.test";
        String body = "{\"name\":\"Registry Viewer\",\"email\":\"" + email + "\",\"password\":\"GoodPassword123\",\"badgeNumber\":\"" + email.hashCode() + "\",\"agency\":\"Registry Agency\"}";
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.user.role").value("Read-Only"))
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(response).get("token").asText();
        mvc.perform(post("/api/criminal-records").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(recordJson("No Write Access", "Fraud")))
                .andExpect(status().isForbidden());
    }

    private String register(String email) throws Exception {
        String body = "{\"name\":\"Registry Officer\",\"email\":\"" + email + "\",\"password\":\"GoodPassword123\",\"badgeNumber\":\"" + email.hashCode() + "\",\"agency\":\"Registry Agency\"}";
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        jdbc.update("UPDATE users SET role = 'Officer' WHERE email = ?", email);
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"GoodPassword123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(login).get("token").asText();
    }

    private String recordJson(String name, String crime) {
        return "{\"name\":\"" + name + "\",\"status\":\"Wanted\",\"riskLevel\":\"Medium\",\"crimeType\":\"" + crime + "\"}";
    }

    private String bearer(String token) { return "Bearer " + token; }
}
