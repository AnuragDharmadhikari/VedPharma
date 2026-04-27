package org.ved.crm.visit;

import java.util.UUID;

public record VisitProductDto(
        UUID id,
        UUID productId,
        String productName,
        String hsnCode,
        Integer samplesGiven,
        String feedback
) {}
