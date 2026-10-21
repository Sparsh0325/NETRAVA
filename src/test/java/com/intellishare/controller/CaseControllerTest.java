package com.intellishare.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellishare.repository.InvestigationCaseRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CaseControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired InvestigationCaseRepository cases;

    @BeforeEach void clearData(){jdbc.update("DELETE FROM case_shares");cases.deleteAll();users.deleteAll();}

    @Test
    void createsAndReadsCaseWithEntitiesAndTimeline() throws Exception {
        String token=register("case-officer@example.test","City Police");
        String json=mvc.perform(post("/api/cases").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content(createJson("FIR-2026-1001","Burglary in Jaipur","City Police")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.caseNumber").value("FIR-2026-1001"))
            .andExpect(jsonPath("$.status").value("Registered")).andExpect(jsonPath("$.entities[0].type").value("Person"))
            .andExpect(jsonPath("$.timeline[0].action").value("Case created"))
            .andReturn().getResponse().getContentAsString();
        assertEquals(1,cases.count());
        String id=mapper.readTree(json).get("id").asText();
        mvc.perform(get("/api/cases/{id}",id).header("Authorization",bearer(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Burglary in Jaipur"));
    }

    @Test
    void filtersAndPaginatesOnlyWithinUsersAgency() throws Exception {
        String token=register("case-search@example.test","North Police");
        mvc.perform(post("/api/cases").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content(createJson("FIR-2026-2001","Fraud inquiry","North Police"))).andExpect(status().isCreated());
        mvc.perform(post("/api/cases").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content(createJson("FIR-2026-2002","Theft inquiry","North Police"))).andExpect(status().isCreated());
        mvc.perform(get("/api/cases?q=fraud&page=0&size=1").header("Authorization",bearer(token)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Fraud inquiry"));
        mvc.perform(get("/api/cases?size=101").header("Authorization",bearer(token)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updatesStatusAddsTimelineAndEvidence() throws Exception {
        String token=register("case-update@example.test","South Police");
        String response=mvc.perform(post("/api/cases").header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content(createJson("FIR-2026-3001","Robbery case","South Police")))
            .andReturn().getResponse().getContentAsString();
        String id=mapper.readTree(response).get("id").asText();
        mvc.perform(put("/api/cases/{id}",id).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Robbery case\",\"type\":\"Robbery\",\"status\":\"Under Investigation\",\"priority\":\"High\",\"entities\":[]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("Under Investigation"))
            .andExpect(jsonPath("$.timeline[0].action").value("Status changed"));
        mvc.perform(post("/api/cases/{id}/evidence",id).header("Authorization",bearer(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Photo\",\"fileType\":\"image/jpeg\",\"sha256Hash\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.evidence[0].title").value("Photo"));
    }

    @Test
    void enforcesAuthenticationAgencyBoundaryAndDeleteRole() throws Exception {
        mvc.perform(get("/api/cases")).andExpect(status().isUnauthorized());
        String owner=register("owner@example.test","West Police");
        String response=mvc.perform(post("/api/cases").header("Authorization",bearer(owner)).contentType(MediaType.APPLICATION_JSON)
                .content(createJson("FIR-2026-4001","Boundary case","West Police")))
            .andReturn().getResponse().getContentAsString();
        String id=mapper.readTree(response).get("id").asText();
        String other=register("other@example.test","East Police");
        mvc.perform(get("/api/cases/{id}",id).header("Authorization",bearer(other))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/cases/{id}",id).header("Authorization",bearer(owner))).andExpect(status().isForbidden());
    }

    @Test
    void caseShareGrantsAgencyScopedReadAndRevocationRemovesAccess() throws Exception {
        String owner=register("sharing-owner@example.test","Metro Police");
        String recipient=register("sharing-recipient@example.test","Harbor Police");
        String response=mvc.perform(post("/api/cases").header("Authorization",bearer(owner)).contentType(MediaType.APPLICATION_JSON)
                .content(createJson("SHARE-2026-1","Shared test case","Metro Police")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id=mapper.readTree(response).get("id").asText();
        mvc.perform(get("/api/cases/{id}",id).header("Authorization",bearer(recipient))).andExpect(status().isNotFound());
        String share=mvc.perform(post("/api/shares/cases/{id}",id).header("Authorization",bearer(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientAgency\":\"Harbor Police\",\"permission\":\"READ\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        long shareId=mapper.readTree(share).get("id").asLong();
        mvc.perform(get("/api/cases/{id}",id).header("Authorization",bearer(recipient))).andExpect(status().isOk());
        mvc.perform(get("/api/cases").header("Authorization",bearer(recipient))).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].caseNumber").value("SHARE-2026-1"));
        mvc.perform(post("/api/cases/{id}/timeline",id).header("Authorization",bearer(recipient)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"action\":\"unauthorized edit\"}")).andExpect(status().isForbidden());
        mvc.perform(delete("/api/shares/{id}",shareId).header("Authorization",bearer(owner))).andExpect(status().isNoContent());
        mvc.perform(get("/api/cases/{id}",id).header("Authorization",bearer(recipient))).andExpect(status().isNotFound());
        String editShare=mvc.perform(post("/api/shares/cases/{id}",id).header("Authorization",bearer(owner)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientAgency\":\"Harbor Police\",\"permission\":\"EDIT\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long editShareId=mapper.readTree(editShare).get("id").asLong();
        mvc.perform(put("/api/cases/{id}",id).header("Authorization",bearer(recipient)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated through share\",\"type\":\"Theft\",\"status\":\"Under Investigation\",\"priority\":\"Medium\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated through share"));
        mvc.perform(delete("/api/shares/{id}",editShareId).header("Authorization",bearer(recipient))).andExpect(status().isNotFound());
    }

    private String register(String email,String agency) throws Exception {
        String body="{\"name\":\"Case Officer\",\"email\":\""+email+"\",\"password\":\"GoodPassword123\",\"badgeNumber\":\""+email.hashCode()+"\",\"agency\":\""+agency+"\"}";
        String response=mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        jdbc.update("UPDATE users SET role = 'Officer' WHERE email = ?", email);
        String login=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\""+email+"\",\"password\":\"GoodPassword123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(login).get("token").asText();
    }
    private String createJson(String number,String title,String agency){
        return "{\"caseNumber\":\""+number+"\",\"title\":\""+title+"\",\"type\":\"Theft\",\"city\":\"Jaipur\",\"priority\":\"Medium\",\"entities\":[{\"type\":\"Person\",\"value\":\"Known person\"}]}";
    }
    private String bearer(String token){return "Bearer "+token;}
}
