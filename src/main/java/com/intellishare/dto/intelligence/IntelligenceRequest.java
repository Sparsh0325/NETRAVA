package com.intellishare.dto.intelligence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IntelligenceRequest(@NotBlank @Size(max = 1000) String prompt, boolean webSearch) { }
