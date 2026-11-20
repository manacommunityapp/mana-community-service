package com.manacommunity.api.platform.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;

/**
 * AOP aspect that intercepts methods annotated with {@link AuditAction} and
 * records an audit journal entry after the method completes successfully.
 *
 * For FINANCIAL category: if the journal write fails, the exception is re-thrown
 * so the calling transaction rolls back. For other categories, failures are logged
 * but do not break the business operation.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditJournalService auditJournalService;

    @Around("@annotation(auditAction)")
    public Object around(ProceedingJoinPoint pjp, AuditAction auditAction) throws Throwable {
        Object result = pjp.proceed();

        try {
            Long communityId = extractCommunityId(pjp);
            String entityId  = extractEntityId(pjp, result);

            switch (auditAction.category()) {
                case FINANCIAL -> auditJournalService.recordFinancial(
                        auditAction.action(), auditAction.entityType(), entityId, communityId, null);
                case SECURITY -> auditJournalService.recordSecurity(
                        auditAction.action(), null, null, null);
                case PRIVACY -> auditJournalService.recordPrivacy(
                        auditAction.action(), null, auditAction.entityType(), entityId, communityId);
                default -> auditJournalService.recordBusiness(
                        auditAction.action(), auditAction.entityType(), entityId, communityId, null);
            }
        } catch (Exception ex) {
            if (auditAction.category() == AuditCategory.FINANCIAL) {
                // Financial audit failure must propagate — cannot silently lose the record
                throw ex;
            }
            log.error("Audit aspect failed for action={} category={}: {}",
                    auditAction.action(), auditAction.category(), ex.getMessage());
        }

        return result;
    }

    private Long extractCommunityId(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Parameter[] params = sig.getMethod().getParameters();
        Object[] args = pjp.getArgs();
        for (int i = 0; i < params.length; i++) {
            if ("communityId".equals(params[i].getName()) && args[i] instanceof Long l) {
                return l;
            }
        }
        // Fall back to first Long param
        for (Object arg : args) {
            if (arg instanceof Long l) return l;
        }
        return null;
    }

    private String extractEntityId(ProceedingJoinPoint pjp, Object returnValue) {
        if (returnValue instanceof Long l) return String.valueOf(l);
        if (returnValue instanceof String s) return s;
        // Try method args
        for (Object arg : pjp.getArgs()) {
            if (arg instanceof Long l) return String.valueOf(l);
            if (arg instanceof String s && !s.isBlank()) return s;
        }
        return null;
    }
}
