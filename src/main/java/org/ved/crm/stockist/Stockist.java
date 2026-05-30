package org.ved.crm.stockist;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.ved.crm.common.audit.BaseAuditEntity;
import org.ved.crm.user.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "stockists")
public class Stockist extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_rep_id",nullable = false)
    private User assignedRep;

    @NotBlank
    @Column(nullable = false)
    private String firmName;

    @NotBlank
    @Column(nullable = false)
    private String ownerName;

    @Column(name = "email")
    private String email;

    @Column(unique = true)
    private String gstin;

    @NotBlank
    @Column(nullable = false)
    private String state;

    @NotBlank
    @Column(nullable = false)
    private String city;

    @Column(columnDefinition = "TEXT")
    private String address;

    @NotBlank
    @Column(nullable = false)
    private String phone;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

}
