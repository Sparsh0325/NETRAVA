package com.intellishare.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String department,
        @Size(max = 50) String phone) { }
