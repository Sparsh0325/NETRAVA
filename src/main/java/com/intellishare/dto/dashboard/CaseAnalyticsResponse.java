package com.intellishare.dto.dashboard;

import java.util.List;

public record CaseAnalyticsResponse(long totalCases, List<Bucket> byType, List<Bucket> byStatus,
                                   List<Bucket> byPriority, List<Bucket> byMonth) {
    public record Bucket(String label, long count) { }
}
