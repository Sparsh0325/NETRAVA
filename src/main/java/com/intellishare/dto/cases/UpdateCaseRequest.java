package com.intellishare.dto.cases;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
public record UpdateCaseRequest(
    @NotBlank @Size(max=255) String title, @Size(max=20000) String description,
    @NotBlank @Size(max=100) String type,
    @NotBlank @Pattern(regexp="Registered|Under Investigation|Closed|Cold Case") String status,
    @NotBlank @Pattern(regexp="High|Medium|Low") String priority,
    LocalDate incidentDate, @Size(max=255) String address, @Size(max=100) String city,
    @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
    @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
    Long assignedOfficerId, @Valid List<EntityInput> entities
) { }
