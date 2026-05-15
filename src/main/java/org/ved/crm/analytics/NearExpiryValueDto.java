package org.ved.crm.analytics;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record NearExpiryValueDto(

        UUID batchId,

        String batchNumber,

        UUID productId,

        String productName,

        LocalDate expiryDate,

        long daysUntilExpiry,

        long currentQuantity,

        BigDecimal dealerPrice,

        BigDecimal valueAtRisk

)implements Serializable {
}
