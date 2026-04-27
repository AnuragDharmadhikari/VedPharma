package org.ved.crm.visit;

import jakarta.persistence.*;
import lombok.*;
import org.ved.crm.common.audit.BaseAuditEntity;
import org.ved.crm.doctor.Doctor;
import org.ved.crm.user.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "visits",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"doctor_id","visit_date"},
                        name = "uk_visits_doctor_date"
                )
        }
)
public class Visit extends BaseAuditEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rep_id", nullable = false)
    private User rep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id",nullable = false)
    private Doctor doctor;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @Builder.Default
    @OneToMany(mappedBy = "visit",cascade = CascadeType.ALL,orphanRemoval = true)
    private List<VisitProduct> visitProducts = new ArrayList<>();
}
