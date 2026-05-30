package org.ved.crm.chemist;

import jakarta.persistence.*;
import lombok.*;
import org.ved.crm.common.audit.BaseAuditEntity;
import org.ved.crm.user.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chemists")
public class Chemist extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_rep_id",nullable = false)
    private User assignedRep;

    @Column(name = "firm_name",nullable = false)
    private String firmName;

    @Column(name = "owner_name",nullable = false)
    private String ownerName;

    @Column(name = "email")
    private String email;

    // Drug License Number — legal requirement for every retail pharmacy in India
    // Must be unique — no two chemists can have the same DL number
    @Column(name = "drug_license_number",nullable = false,unique = true)
    private String drugLicenseNumber;

    @Column(name = "gstin",unique = true)
    private String gstin;

    // State is critical — used for CGST+SGST vs IGST determination on invoices
    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
