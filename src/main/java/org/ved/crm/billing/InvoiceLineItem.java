package org.ved.crm.billing;

import jakarta.persistence.*;
import lombok.*;
import org.ved.crm.common.audit.BaseAuditEntity;
import org.ved.crm.product.Product;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoice_line_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceLineItem extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id",nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id",nullable = false)
    private Product product;

    @Column(name = "hsn_code",nullable = false)
    private String hsnCode;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "discount_pct", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountPct;

    @Column(name = "taxable_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxableAmount;

    @Column(name = "cgst_amt", nullable = false, precision = 10, scale = 2)
    private BigDecimal cgstAmt;

    @Column(name = "sgst_amt", nullable = false, precision = 10, scale = 2)
    private BigDecimal sgstAmt;

    @Column(name = "igst_amt", nullable = false, precision = 10, scale = 2)
    private BigDecimal igstAmt;

    @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;

    @Builder.Default
    @Column(name = "free_quantity")
    private Integer freeQuantity = 0;

    // Batch number from which this quantity was deducted
    // Nullable — existing invoices don't have this info
    // Always populated for new invoices after this feature
    @Column(name = "batch_number")
    private String batchNumber;

    // Expiry date of the batch — required on pharma GST invoices
    // Nullable — same reason as batchNumber
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

}
