package com.intellishare.audit;

import java.util.List;

public record AuditPage(List<AuditEvent> content, int page, int size, long totalElements, int totalPages) { }
