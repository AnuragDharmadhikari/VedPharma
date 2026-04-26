package org.ved.crm.product;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.ved.crm.common.audit.BaseAuditEntity;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "products")
public class Product extends BaseAuditEntity {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String molecule;

    @NotBlank
    @Column(nullable = false)
    private String category;

    @NotBlank
    @Column(nullable = false)
    private String hsnCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GstRate gstRate;

    @NotNull
    @Positive
    @Column(nullable = false,precision = 10,scale = 2)
    private BigDecimal mrp;

    @NotNull
    @Positive
    @Column(nullable = false,precision = 10,scale = 2)
    private BigDecimal dealerPrice;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
}
