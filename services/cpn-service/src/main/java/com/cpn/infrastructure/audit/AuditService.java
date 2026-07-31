package com.cpn.infrastructure.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Aspect
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    public void logAction(String action, String entityType, UUID entityId, String oldValue, String newValue) {
        // Logic to persist audit log to audit_logs table
        log.info("AUDIT: Action: {}, EntityType: {}, EntityID: {}, OldValue: {}, NewValue: {}",
                action, entityType, entityId, oldValue, newValue);
    }

    // Example pointcut to hook onto service methods if we create custom @Auditable annotation
    @AfterReturning(pointcut = "@annotation(org.springframework.transaction.annotation.Transactional)", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.debug("Method {} returned {}", joinPoint.getSignature().getName(), result);
    }
}
