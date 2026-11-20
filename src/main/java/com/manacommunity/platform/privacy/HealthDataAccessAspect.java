package com.manacommunity.platform.privacy;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AOP aspect that intercepts {@link HealthDataAccess}-annotated service methods
 * and records every invocation in {@code health_data_access_log}.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class HealthDataAccessAspect {

    private final HealthDataAccessLogger accessLogger;

    @Around("@annotation(healthDataAccess)")
    public Object logAccess(ProceedingJoinPoint pjp,
                            HealthDataAccess healthDataAccess) throws Throwable {
        Object result = pjp.proceed();

        // Log after successful proceed — don't block exceptions from propagating
        try {
            Long accessorId = currentUserId();
            String accessorType = accessorId != null ? "USER" : "SYSTEM";
            Long subjectUserId = extractSubjectUserId(pjp.getArgs());

            if (subjectUserId != null) {
                accessLogger.log(
                        accessorId,
                        accessorType,
                        subjectUserId,
                        healthDataAccess.recordType(),
                        extractRecordId(pjp.getArgs()),
                        null,
                        healthDataAccess.purpose(),
                        clientIp()
                );
            }
        } catch (Exception e) {
            log.warn("HealthDataAccessAspect: logging failed: {}", e.getMessage());
        }

        return result;
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UsernamePasswordAuthenticationToken token) {
            Object principal = token.getPrincipal();
            if (principal instanceof String s) {
                try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    /** Looks for a Long argument (ID) in the method args — first Long wins as subjectUserId. */
    private Long extractSubjectUserId(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof Long l) return l;
        }
        return null;
    }

    /** Second Long arg, if present, is treated as the record ID. */
    private String extractRecordId(Object[] args) {
        if (args == null) return "unknown";
        int longCount = 0;
        for (Object arg : args) {
            if (arg instanceof Long l) {
                longCount++;
                if (longCount == 2) return String.valueOf(l);
            }
        }
        return "unknown";
    }

    private String clientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                int comma = xff.indexOf(',');
                return (comma > 0 ? xff.substring(0, comma) : xff).trim();
            }
            return req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
