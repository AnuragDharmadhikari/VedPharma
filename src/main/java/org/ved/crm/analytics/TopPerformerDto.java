package org.ved.crm.analytics;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record TopPerformerDto(

        UUID id,

        String name,

        String state,

        BigDecimal totalRevenue,

        long invoiceCount

) implements Serializable {
}
