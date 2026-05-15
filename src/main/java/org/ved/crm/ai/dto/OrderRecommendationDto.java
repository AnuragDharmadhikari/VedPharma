package org.ved.crm.ai.dto;

import java.io.Serializable;

public record OrderRecommendationDto(
        String chemistId,
        String chemistName,

        String recommendedProducts,
        String recommendedProductsMr,

        String reasoning,
        String reasoningMr,

        String applicableSchemes,
        String applicableSchemesMr,

        String estimatedOrderValue
) implements Serializable {}