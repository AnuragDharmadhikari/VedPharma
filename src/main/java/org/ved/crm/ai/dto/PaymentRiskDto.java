package org.ved.crm.ai.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record PaymentRiskDto(
        String partyId ,
        String partyName ,
        String riskLevel,
        int riskScore,
        BigDecimal totalOutstanding,
        String averagePaymentDays,

        String riskAnalysis,
        String riskAnalysisMr,

        String recommendedAction,
        String recommendedActionMr
)implements Serializable {}