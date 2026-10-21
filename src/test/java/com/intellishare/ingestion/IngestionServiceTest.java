package com.intellishare.ingestion;

import com.intellishare.model.User;
import com.intellishare.repository.InvestigationCaseRepository;
import com.intellishare.repository.UserRepository;
import com.intellishare.service.IngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @ActiveProfiles("test")
class IngestionServiceTest {
    @Autowired IngestionService ingestion;
    @Autowired UserRepository users;
    @Autowired InvestigationCaseRepository cases;
    @Autowired JdbcTemplate jdbc;

    @Test
    void importsValidatedRowsAndPersistsJobMetadata() {
        User actor=officer();
        var file=new MockMultipartFile("file","cases.csv","text/csv",("caseNumber,title,type,status,priority,incidentDate,city,address,description\n"+
                "CSV-TEST-1,Imported demo,Theft,Registered,Medium,2026-01-02,Delhi,Main Road,synthetic fixture\n").getBytes());
        var result=ingestion.importCases(actor,file);
        assertEquals("SUCCEEDED",result.status()); assertEquals(1,result.processedRows());
        assertTrue(cases.existsByCaseNumberIgnoreCase("CSV-TEST-1"));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM ingestion_jobs WHERE id=? AND status='SUCCEEDED'",Integer.class,result.jobId()));
    }

    @Test
    void rejectsInvalidRowsWithoutCreatingCasesAndRecordsFailedJob() {
        User actor=officer();
        var file=new MockMultipartFile("file","bad.csv","text/csv",("caseNumber,title,type,status,priority,incidentDate,city,address,description\n"+
                "BAD-TEST-1,Invalid,Theft,Unknown,Medium,,,,bad row\n").getBytes());
        var result=ingestion.importCases(actor,file);
        assertEquals("FAILED",result.status()); assertFalse(result.errors().isEmpty());
        assertFalse(cases.existsByCaseNumberIgnoreCase("BAD-TEST-1"));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM ingestion_jobs WHERE id=? AND status='FAILED'",Integer.class,result.jobId()));
    }

    private User officer() {
        String suffix=java.util.UUID.randomUUID().toString();
        User user=users.saveAndFlush(new User("CSV Test Officer",suffix+"@test.invalid","hash","CSV-"+suffix.substring(0,8),"Test Agency",null,null));
        jdbc.update("UPDATE users SET role='Officer' WHERE id=?",user.getId());
        return users.findById(user.getId()).orElseThrow();
    }
}
