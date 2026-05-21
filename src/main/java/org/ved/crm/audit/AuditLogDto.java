package org.ved.crm.audit;

import java.io.Serializable;

public record AuditLogDto(
        String id,
        String userId,
        String userEmail,
        String action,
        String entityId,
        String entityType,
        String result,
        String errorMessage,
        String createdAt,
        String updatedAt
) implements Serializable {}