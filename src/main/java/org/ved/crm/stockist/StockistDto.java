package org.ved.crm.stockist;

import java.time.Instant;
import java.util.UUID;

public record StockistDto(
        UUID id,
        UUID assignedRepId,
        String assignedRepName,
        String firmName,
        String ownerName,
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