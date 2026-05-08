package org.ved.crm.returns;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReturnDto(
        UUID id,
        String returnNumber,
        UUID chemistId,
        String chemistFirmName,
        UUID stockistId,
        String stockistFirmName,
        LocalDate returnDate,
        String reason,
        ReturnStatus status,
        List<ReturnItemDto> returnItems,
        CreditNoteDto creditNote,
        Instant createdAt,
        Instant updatedAt
) {
}
