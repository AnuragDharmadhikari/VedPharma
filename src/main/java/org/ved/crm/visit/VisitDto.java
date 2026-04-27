package org.ved.crm.visit;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VisitDto(
        UUID id,
        UUID repId,
        String repName,
        UUID doctorId,
        String doctorName,
        String doctorSpecialty,
        LocalDate visitDate,
        VisitStatus status,
        String notes,
        String aiSummary,
        List<VisitProductDto> visitProducts,
        Instant createdAt,
        Instant updatedAt
) {
}
