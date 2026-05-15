package org.ved.crm.analytics;

import java.io.Serializable;
import java.math.BigDecimal;

public record OpenCreditNoteTotalDto(

        BigDecimal totalOpenValue,

        long openCount,

        BigDecimal stockistOpenValue,

        BigDecimal chemistOpenValue

) implements Serializable {
}
