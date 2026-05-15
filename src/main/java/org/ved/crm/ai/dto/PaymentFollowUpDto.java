package org.ved.crm.ai.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record PaymentFollowUpDto(
        String invoiceId,
        String invoiceNumber,
        String billedToName,
        long daysOverdue,
        BigDecimal outstandingAmount,
        String messageTone,

        String followUpMessage,
        String followUpMessageMr
) implements Serializable {}