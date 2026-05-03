package org.ved.crm.inventory;

import jakarta.persistence.*;
import lombok.*;
import org.ved.crm.common.audit.BaseAuditEntity;
import org.ved.crm.product.Product;

import java.time.LocalDate;

@Entity
@Table(
        name = "batches",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_batches_product_batch_number",
                columnNames = {"product_id", "batch_number"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batch extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id",nullable = false)
    private Product product;

    @Column(name = "batch_number",nullable = false)
    private String batchNumber;

    @Column(name = "mfg_date",nullable = false)
    private LocalDate mfgDate;

    @Column(name = "expiry_date",nullable = false)
    private LocalDate expiryDate;

    @Column(name = "initial_quantity",nullable = false)
    private Integer initialQuantity;

    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity;

    public boolean isExpired(){
        return LocalDate.now().isAfter(expiryDate);
    }

    public boolean isNearExpiry(){
        return !LocalDate.now().plusDays(90).isBefore(expiryDate)
                && !isExpired();
    }
}
