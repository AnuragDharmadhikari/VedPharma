package org.ved.crm.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @PreAuthorize("hasRole('OWNER')")
    public List<AuditLogDto> getAllLogs() {
        return auditLogRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(log -> new AuditLogDto(
                        log.getId().toString(),
                        log.getUserId() != null ? log.getUserId().toString() : null,
                        log.getUserEmail(),
                        log.getAction(),
                        log.getEntityId() != null ? log.getEntityId().toString() : null,
                        log.getEntityType(),
                        log.getResult().name(),
                        log.getErrorMessage(),
                        log.getCreatedAt().toString(),
                        log.getUpdatedAt().toString()
                ))
                .toList();
    }
}