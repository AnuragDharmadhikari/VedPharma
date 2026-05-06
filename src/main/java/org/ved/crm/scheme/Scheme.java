package org.ved.crm.scheme;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.cglib.core.Local;
import org.ved.crm.chemist.Chemist;
import org.ved.crm.common.audit.BaseAuditEntity;
import org.ved.crm.product.Product;
import org.ved.crm.stockist.Stockist;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "schemes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scheme extends BaseAuditEntity {

    // The product this scheme applies to.
    // A scheme is always product-specific — you don't give a blanket
    // deal across all products, you negotiate per product per buyer.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Buyer — XOR: exactly one of these two must be set.
    // VIA_STOCKIST orders → stockist gets the scheme benefit.
    // DIRECT orders → chemist gets the scheme benefit.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stockist_id")
    private Stockist stockist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chemist_id")
    private Chemist chemist;

    @Enumerated(EnumType.STRING)
    @Column(name = "scheme_type",nullable = false)
    private SchemeType schemeType;

    @Column(name = "min_quantity" , nullable = false)
    private Integer minQuantity;

    @Column(name = "free_quantity")
    private Integer freeQuantity;

    @Column(name = "discount_pct",precision = 5,scale = 2)
    private BigDecimal discountPct;

    @Column(name = "valid_from",nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to" , nullable = false)
    private LocalDate validTo;

    @Builder.Default
    @Column(name = "is_active",nullable = false)
    private boolean isActive = true;

}
