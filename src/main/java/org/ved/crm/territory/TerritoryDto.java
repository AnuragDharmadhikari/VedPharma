package org.ved.crm.territory;

import java.time.Instant;
import java.util.UUID;

public record TerritoryDto(
        UUID id,
        String name,
        String state,
        String zone,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt

) {
}
