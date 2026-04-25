package org.ved.crm.doctor;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.ved.crm.common.audit.BaseAuditEntity;
import org.ved.crm.territory.Territory;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "doctors")
public class Doctor extends BaseAuditEntity {

    @NotBlank
    @Column(nullable = false)
    private String fullName;

    @NotBlank
    @Column(nullable = false)
    private String specialty;

    private String hospitalName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DoctorTier tier;

    private String phone;

    @Email
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String city;

    @NotBlank
    @Column(nullable = false)
    private String state;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "territory_id")
    private Territory territory;
}
