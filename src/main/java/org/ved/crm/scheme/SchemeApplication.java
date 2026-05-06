package org.ved.crm.scheme;

import jakarta.persistence.*;
import lombok.*;
import org.ved.crm.common.audit.BaseAuditEntity;
import org.ved.crm.order.OrderItem;

import java.math.BigDecimal;

@Entity
@Table(name = "scheme_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeApplication extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private OrderItem orderItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id",nullable = false)
    private Scheme scheme;

    @Enumerated(EnumType.STRING)
    @JoinColumn(name = "scheme_type",nullable = false)
    private SchemeType schemeType;

    @Column(name = "benefit_description", nullable = false)
    private String benefitDescription;

    @Column(name = "free_quantity")
    private Integer freeQuantity;

    @Column(name = "discount_applied",precision = 5,scale = 2)
    private BigDecimal discountApplied;

}
