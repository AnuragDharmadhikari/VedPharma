package org.ved.crm.analytics;

import java.io.Serializable;
import java.math.BigDecimal;

public record ReturnsSummaryDto(

        String month,

        long totalReturnCount,

        long processedReturnCount,

        long rejectedReturnCount,

        BigDecimal totalReturnValue,

        BigDecimal chemistReturnValue,

        BigDecimal stockistReturnValue

) implements Serializable {
}
