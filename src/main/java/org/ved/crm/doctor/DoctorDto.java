package org.ved.crm.doctor;

import java.time.Instant;
import java.util.UUID;

public record DoctorDto(
        UUID id,
        String fullName,
        String specialty,
        String hospitalName,
        DoctorTier tier,
        String phone,
        String email,
        String city,
        String state,
        UUID territoryId,
        String territoryName,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt

) {
}
