package com.intellishare.audit;

import java.time.OffsetDateTime;

public record AuditEvent(long id, long chainIndex, OffsetDateTime timestamp, String actorUsername,
                         String actorRole, String actorBadgeNumber, String agency, String action,
                         String resource, String details, String previousHash, String hash, int hashVersion) { }
