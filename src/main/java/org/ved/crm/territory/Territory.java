package org.ved.crm.territory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.ved.crm.common.audit.BaseAuditEntity;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "territories")
public class Territory extends BaseAuditEntity {

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String state;

    private String zone;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
}
