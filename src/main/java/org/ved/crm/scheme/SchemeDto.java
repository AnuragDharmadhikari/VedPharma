package org.ved.crm.scheme;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SchemeDto(

        UUID id,

        UUID productId,
        String productName,
        String productMolecule,

        UUID chemistId,
        String chemistFirmName,

        UUID stockistId,
        String stockistFirmName,

        SchemeType schemeType,
        Integer minQuantity,

        Integer freeQuantity,

        BigDecimal discountPct,

        LocalDate validFrom,
        LocalDate validTo,

        boolean isActive,

        Instant createdAt,
        Instant updatedAt

) {
}
