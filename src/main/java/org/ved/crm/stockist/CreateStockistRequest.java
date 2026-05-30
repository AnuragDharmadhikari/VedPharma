package org.ved.crm.stockist;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateStockistRequest(
        @NotNull UUID assignedRepId,
        @NotBlank String firmName,
        @NotBlank String ownerName,
        @Pattern(regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
                message = "Invalid GSTIN format")
        String gstin,
        @NotBlank String state,
        @NotBlank String city,
        String address,
        @NotBlank String phone,

        // Optional — used for sending invoice emails
        @Email(message = "Invalid email format")
        String email
) {}