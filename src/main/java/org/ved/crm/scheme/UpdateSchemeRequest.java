package org.ved.crm.scheme;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateSchemeRequest(

        @Min(value = 1, message = "Minimum quantity must be at least 1")
        Integer minQuantity,

        @Min(value = 1, message = "Free quantity must be at least 1")
        Integer freeQuantity,

        @DecimalMin(value = "0.01", message = "Discount percentage must be greater than 0")
        @DecimalMax(value = "100.00", message = "Discount percentage cannot exceed 100")
        BigDecimal discountPct,

        LocalDate validTo,

        Boolean isActive
) {
}
