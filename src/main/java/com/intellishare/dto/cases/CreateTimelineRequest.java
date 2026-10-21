package com.intellishare.dto.cases;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CreateTimelineRequest(@NotBlank @Size(max=255) String action, @Size(max=10000) String details) { }
