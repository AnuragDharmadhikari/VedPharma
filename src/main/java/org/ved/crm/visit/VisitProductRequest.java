package org.ved.crm.visit;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VisitProductRequest(
        @NotNull UUID productId,
        Integer samplesGiven,
        String feedback
) {
}
