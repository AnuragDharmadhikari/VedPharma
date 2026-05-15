package org.ved.crm.ai.dto;

import java.io.Serializable;

public record DoctorEngagementDto(
        String doctorId,
        String doctorName,
        int engagementScore,
        String engagementLevel,

        // English analysis
        String analysis,
        // Marathi analysis
        String analysisMr,

        // English recommendations
        String recommendations,
        // Marathi recommendations
        String recommendationsMr
) implements Serializable {}