package com.intellishare.dto.sharing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateShareRequest(@NotBlank @Size(max=100) String recipientAgency,
                                 @Pattern(regexp="READ|EDIT") String permission) { }
