package com.dongbang.finance.application.facade;

import com.dongbang.finance.domain.AuditLog;
import com.dongbang.finance.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class AuditLogFacade {
    private final AuditLogRepository auditLogRepository;

    public void record(Long organizationId, Long actorId, String action, String entityType,
                       Long entityId, String before, String after) {
        auditLogRepository.save(new AuditLog(organizationId, actorId, action, entityType, entityId, before, after));
    }

    public void record(Long organizationId, Long actorId, String action, String entityType,
                       Long entityId, String before, String after, String reason) {
        auditLogRepository.save(new AuditLog(
                organizationId, actorId, action, entityType, entityId, before, after, reason));
    }
}
