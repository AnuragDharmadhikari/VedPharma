package org.ved.crm.billing;

import java.math.BigDecimal;
import java.time.LocalDate;

// Internal DTO for PDF generation only
// Includes MRP and GST rate from product — not in standard InvoiceLineItemDto
record InvoiceLineItemPdfDto(
        String productName,
        String hsnCode,
        BigDecimal mrp,
        Integer quantity,
        Integer freeQuantity,
        BigDecimal unitPrice,
        BigDecimal discountPct,
        BigDecimal taxableAmount,
        BigDecimal cgstAmt,
        BigDecimal sgstAmt,
        BigDecimal igstAmt,
        BigDecimal lineTotal,
        String batchNumber,
        LocalDate expiryDate,
        int gstRate
) {}