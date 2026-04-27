package org.ved.crm.visit;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateVisitRequest(
        @NotNull UUID repId,
        @NotNull UUID doctorId,
        @NotNull LocalDate visitDate,
        @NotNull VisitStatus status,
        String notes,
        List<VisitProductRequest> products
        ) {
}
