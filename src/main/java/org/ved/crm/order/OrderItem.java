package org.ved.crm.order;

import jakarta.persistence.*;
import lombok.*;
import org.ved.crm.common.audit.BaseAuditEntity;
import org.ved.crm.product.Product;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id",nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id",nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false,precision = 10,scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false,precision = 10,scale = 2)
    private BigDecimal discountPct;

    @Column(nullable = false,precision = 10,scale = 2)
    private BigDecimal lineTotal;

    @Builder.Default
    @Column(name = "scheme_discount_pct", precision = 5, scale = 2)
    private BigDecimal schemeDiscountPct = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "free_quantity")
    private Integer freeQuantity = 0;

}
