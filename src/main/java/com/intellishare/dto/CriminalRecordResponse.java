package com.intellishare.dto;

import com.intellishare.model.CriminalRecord;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CriminalRecordResponse(
        Long id, String recordNumber, String name, String alias, LocalDate dateOfBirth, Integer age,
        String gender, String status, String riskLevel, String crimeType, String lastSeenLocation,
        String notes, boolean syntheticDemo, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
    public static CriminalRecordResponse from(CriminalRecord record) {
        return new CriminalRecordResponse(record.getId(), record.getRecordNumber(), record.getName(),
                record.getAlias(), record.getDateOfBirth(), record.getAge(), record.getGender(), record.getStatus(),
                record.getRiskLevel(), record.getCrimeType(), record.getLastSeenLocation(), record.getNotes(),
                record.isSyntheticDemo(), record.getCreatedAt(), record.getUpdatedAt());
    }
}
