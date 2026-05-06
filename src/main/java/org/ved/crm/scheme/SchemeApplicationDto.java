package org.ved.crm.scheme;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SchemeApplicationDto(

        UUID id,

        UUID orderItemId,

        UUID schemeId,

        SchemeType schemeType,

        String benefitDescription,

        Integer freeQuantity,

        BigDecimal discountApplied,

        Instant createdAt
) {
}
