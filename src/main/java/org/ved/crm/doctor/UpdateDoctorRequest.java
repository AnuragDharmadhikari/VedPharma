package org.ved.crm.doctor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateDoctorRequest(
        @NotBlank String fullName,
        @NotBlank String specialty,
        String hospitalName,
        @NotNull DoctorTier tier,
        String phone,
        @Email String email,
        @NotBlank String city,
        @NotBlank String state,
        UUID territoryId,
        Boolean isActive
) {}