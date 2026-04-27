package org.ved.crm.visit;

import jakarta.persistence.*;
import lombok.*;
import org.ved.crm.common.audit.BaseAuditEntity;
import org.ved.crm.product.Product;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "visit_products")
public class VisitProduct extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id",nullable = false)
    private Visit visit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id" , nullable = false)
    private Product product;

    private Integer samplesGiven;

    @Column(columnDefinition = "TEXT")
    private String feedback;
}
