package org.ved.crm.chemist;

import java.time.Instant;
import java.util.UUID;

public record ChemistDto(
        UUID id,
        UUID assignedRepId,
        String assignedRepName,
        String firmName,
        String ownerName,
        String drugLicenseNumber,
        String gstin,
        String state,
        String city,
        String address,
        String phone,
        String email,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {}