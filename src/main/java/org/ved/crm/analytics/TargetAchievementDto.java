package org.ved.crm.analytics;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public record TargetAchievementDto(

        UUID repId,

        String repName,

        int month,

        int year,

        int targetVisits,

        int actualVisits,

        int remainingVisits,

        BigDecimal achievementPct,

        boolean targetMet

) implements Serializable {
}
