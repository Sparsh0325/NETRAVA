package com.intellishare.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotBlank @Size(max = 50) String badgeNumber,
        @NotBlank @Size(max = 100) String agency,
        @Size(max = 100) String department,
        @Size(max = 50) String phone
) { }
