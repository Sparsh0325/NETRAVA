package com.intellishare.dto.cases;
import jakarta.validation.constraints.*;
public record CreateEvidenceRequest(
    @NotBlank @Size(max=255) String title, @Size(max=10000) String description,
    @Size(max=500) String fileUrl, @Size(max=100) String fileType,
    @PositiveOrZero Long fileSizeBytes, @Pattern(regexp="(?i)[a-f0-9]{64}") String sha256Hash,
    @Size(max=100) String blockchainTxId
) { }
