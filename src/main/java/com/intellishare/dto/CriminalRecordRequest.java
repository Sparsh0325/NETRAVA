package com.intellishare.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CriminalRecordRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 150) String alias,
        @Past LocalDate dateOfBirth,
        @Min(0) @Max(130) Integer age,
        @Size(max = 20) String gender,
        @NotBlank @Pattern(regexp = "Wanted|In Custody|Surveillance|Released") String status,
        @NotBlank @Pattern(regexp = "High|Medium|Low") String riskLevel,
        @Size(max = 100) String crimeType,
        @Size(max = 255) String lastSeenLocation,
        @Size(max = 10000) String notes
) { }
