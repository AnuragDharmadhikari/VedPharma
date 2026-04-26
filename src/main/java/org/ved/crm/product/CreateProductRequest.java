package org.ved.crm.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank String name,
        @NotBlank String molecule,
        @NotBlank String category,
        @NotBlank String hsnCode,
        @NotNull GstRate gstRate,
        @NotNull @Positive BigDecimal mrp,
        @NotNull @Positive BigDecimal dealerPrice
        ) {
}
