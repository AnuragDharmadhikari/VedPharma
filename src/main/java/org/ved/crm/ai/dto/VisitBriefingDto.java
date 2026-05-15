package org.ved.crm.ai.dto;

import java.io.Serializable;

public record VisitBriefingDto(
        String visitId,
        String doctorName,

        String lastVisitSummary,
        String lastVisitSummaryMr,

        String productFocus,
        String productFocusMr,

        String talkingPoints,
        String talkingPointsMr,

        String activeSchemes,
        String activeSchemesMr,

        String visitStrategy,
        String visitStrategyMr
) implements Serializable {}