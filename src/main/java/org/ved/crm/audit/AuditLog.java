package org.ved.crm.audit;

import jakarta.persistence.*;
import lombok.*;
import org.ved.crm.common.audit.BaseAuditEntity;

import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseAuditEntity {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_email", nullable = false)
    private String userEmail;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "entity_type")
    private String entityType;

    @Enumerated(EnumType.STRING)
    @Column(name = "result" , nullable = false)
    private AuditResult result;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

}
