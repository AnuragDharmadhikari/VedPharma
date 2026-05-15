package org.ved.crm.analytics;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductVelocityDto(

        UUID productId,

        String productName,

        String molecule,

        String hsnCode,

        long totalUnitsSold,

        long totalFreeUnits,

        long totalUnitsDeducted,

        BigDecimal totalRevenue

) implements Serializable {
}
