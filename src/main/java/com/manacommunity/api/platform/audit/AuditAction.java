package com.manacommunity.api.platform.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative auditing for service methods.
 * Applied by {@link AuditAspect}.
 *
 * Example:
 * {@code @AuditAction(action = "APPROVE_REFUND", category = AuditCategory.FINANCIAL, entityType = "PaymentOrder")}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAction {
    String action();
    AuditCategory category() default AuditCategory.BUSINESS;
    String entityType() default "";
}
