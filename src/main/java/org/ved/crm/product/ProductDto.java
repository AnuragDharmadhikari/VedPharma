package org.ved.crm.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        String molecule,
        String category,
        String hsnCode,
        GstRate gstRate,
        int gstRateValue,
        BigDecimal mrp,
        BigDecimal dealerPrice,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}
