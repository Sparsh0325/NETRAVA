package com.intellishare.audit;

import com.intellishare.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@SpringBootTest
@ActiveProfiles("test")
class AuditServiceTest {
    @Autowired AuditService audit;

    @Test
    void appendsAndVerifiesHashChainAndScopesResultsToAgency() {
        User actor = new User("Audit Tester", "audit@test.invalid", "not-a-real-password", "AUD-1", "Unit A", null, null);
        audit.record(actor, "CASE_CREATE", "case:DEMO-1", "Synthetic test event");
        audit.record(actor, "CASE_UPDATE", "case:DEMO-1", "Synthetic test event");
        assertTrue(audit.verify());
        AuditPage page = audit.list(actor, 0, 10, "DEMO-1");
        assertEquals(2, page.totalElements());
        assertTrue(page.content().stream().allMatch(event -> "Unit A".equals(event.agency())));
    }

    @Test
    void versionTwoHashEncodingSeparatesFieldBoundariesAndNullFromEmpty() {
        var at = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        String splitOne = AuditService.hashV2(1, at, "a\nb", "c", null, "agency", "action", "resource", "details", "0".repeat(64));
        String splitTwo = AuditService.hashV2(1, at, "a", "b\nc", null, "agency", "action", "resource", "details", "0".repeat(64));
        String nullField = AuditService.hashV2(1, at, null, "role", null, "agency", "action", "resource", "details", "0".repeat(64));
        String emptyField = AuditService.hashV2(1, at, "", "role", null, "agency", "action", "resource", "details", "0".repeat(64));
        assertNotEquals(splitOne, splitTwo);
        assertNotEquals(nullField, emptyField);
    }
}
