package com.intellishare.dto.cases;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record EntityInput(@NotBlank @Pattern(regexp="Person|Location|Vehicle|Phone|Organization") String type,
                          @NotBlank @Size(max=255) String value, @Size(max=10000) String details) { }
