package com.manacommunity.api.audit;

import com.manacommunity.api.model.AuditLog;
import com.manacommunity.api.repository.AuditLogRepository;
import com.manacommunity.api.security.AuditAction;
import com.manacommunity.api.security.AuditModule;
import com.manacommunity.api.security.CorrelationIdFilter;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Centralized, tamper-resistant audit logging service for mana-community-service.
 * Enforces the 11-dimension audit invariant:
 * WHO, WHAT, WHEN, WHERE, TENANT, RESOURCE, OLD VALUE, NEW VALUE, IP, DEVICE, CORRELATION ID.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TamperResistantAuditService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private static final String DEFAULT_HMAC_SECRET = "mana-audit-tamper-resistant-immutable-secret-key-2026";
    private static final String GENESIS_PREV_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditLogRepository auditLogRepository;

    public static String sha256Hex(String data) {
        if (data == null) return "NULL";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm unavailable", e);
        }
    }

    public static String hmacSha256Hex(String key, String data) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] signedBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : signedBytes) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 signing failed", e);
        }
    }

    public static String computeCanonicalPayload(String tenantId, Long sequenceNumber, LocalDateTime createdAt,
                                                 Long userId, String username, String action, String module,
                                                 String entityName, String entityId, String oldValue, String newValue,
                                                 String correlationId, String ipAddress) {
        return String.join("|",
                tenantId != null ? tenantId : "GLOBAL",
                String.valueOf(sequenceNumber),
                String.valueOf(createdAt),
                String.valueOf(userId),
                username != null ? username : "ANONYMOUS",
                action != null ? action : "UNKNOWN",
                module != null ? module : "UNKNOWN",
                entityName != null ? entityName : "UNKNOWN",
                entityId != null ? entityId : "UNKNOWN",
                sha256Hex(oldValue),
                sha256Hex(newValue),
                correlationId != null ? correlationId : "-",
                ipAddress != null ? ipAddress : "-"
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized AuditLog record(AuditAction action, AuditModule module, String resourceType, String resourceId,
                                        String oldValueJson, String newValueJson) {
        return record(null, action, module, resourceType, resourceId, oldValueJson, newValueJson, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public synchronized AuditLog record(String tenantId, AuditAction action, AuditModule module,
                                        String resourceType, String resourceId,
                                        String oldValueJson, String newValueJson,
                                        String serviceName, String endpoint) {
        try {
            AuditContext ctx = AuditContextHolder.getContext();
            String effectiveTenant = tenantId != null ? tenantId : (ctx.getTenantId() != null ? ctx.getTenantId() : resolveTenantId());
            Long userId = ctx.getUserId() != null ? ctx.getUserId() : currentUserId();
            String username = ctx.getUsername() != null ? ctx.getUsername() : currentUsername();
            String userRole = ctx.getUserRole() != null ? ctx.getUserRole() : currentUserRole();
            String ip = ctx.getIpAddress() != null ? ctx.getIpAddress() : clientIp();
            String userAgent = ctx.getUserAgent() != null ? ctx.getUserAgent() : userAgent();
            String deviceInfo = ctx.getDeviceInfo() != null ? ctx.getDeviceInfo() : deviceInfo(userAgent);
            String correlationId = ctx.getCorrelationId() != null ? ctx.getCorrelationId() : resolveCorrelationId();
            String effectiveService = serviceName != null ? serviceName : (ctx.getServiceName() != null ? ctx.getServiceName() : "mana-community-service");
            String effectiveEndpoint = endpoint != null ? endpoint : (ctx.getEndpoint() != null ? ctx.getEndpoint() : currentEndpoint());
            String location = ctx.getLocation();

            LocalDateTime now = LocalDateTime.now();

            // 1. Fetch latest record for tenant to chain hash
            Optional<AuditLog> latestOpt = auditLogRepository.findTopByTenantIdOrderBySequenceNumberDesc(effectiveTenant);
            long nextSequence = latestOpt.map(a -> a.getSequenceNumber() != null ? a.getSequenceNumber() + 1 : 1L).orElse(1L);
            String prevHash = latestOpt.map(AuditLog::getRecordHash).orElse(GENESIS_PREV_HASH);

            // 2. Compute canonical payload & cryptographic hashes
            String canonicalPayload = computeCanonicalPayload(effectiveTenant, nextSequence, now, userId, username,
                    action.name(), module.name(), resourceType, resourceId, oldValueJson, newValueJson, correlationId, ip);

            String recordHash = hmacSha256Hex(DEFAULT_HMAC_SECRET, prevHash + "::" + canonicalPayload);
            String signature = hmacSha256Hex(DEFAULT_HMAC_SECRET + "_SIGN", recordHash + "::" + nextSequence);

            AuditLog logEntry = AuditLog.builder()
                    .tenantId(effectiveTenant)
                    .userId(userId)
                    .username(username)
                    .userRole(userRole)
                    .action(action.name())
                    .module(module.name())
                    .createdAt(now)
                    .serviceName(effectiveService)
                    .endpoint(effectiveEndpoint)
                    .location(location)
                    .entityName(resourceType)
                    .entityId(resourceId)
                    .oldValue(oldValueJson)
                    .newValue(newValueJson)
                    .ipAddress(ip)
                    .deviceInfo(deviceInfo)
                    .userAgent(userAgent)
                    .correlationId(correlationId)
                    .sequenceNumber(nextSequence)
                    .prevHash(prevHash)
                    .recordHash(recordHash)
                    .signature(signature)
                    .build();

            AuditLog saved = auditLogRepository.save(logEntry);

            AUDIT.info("AUDIT_ENTRY tenant={} seq={} action={} module={} resource={} resourceId={} user={} ip={} hash={}",
                    effectiveTenant, nextSequence, action.name(), module.name(), resourceType, resourceId, username, ip, recordHash.substring(0, 12));

            return saved;
        } catch (Exception ex) {
            log.error("CRITICAL: Failed to persist tamper-resistant audit entry: action={}, resource={}, error={}",
                    action, resourceType, ex.getMessage(), ex);
            return null;
        }
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUsername();
        }
        return "SYSTEM";
    }

    private String currentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            return auth.getAuthorities().iterator().next().getAuthority();
        }
        return "SYSTEM";
    }

    private String resolveTenantId() {
        HttpServletRequest req = currentRequest();
        if (req != null) {
            String tenant = req.getHeader("X-Tenant-Id");
            if (tenant == null || tenant.isBlank()) {
                tenant = req.getHeader("X-Community-Id");
            }
            if (tenant != null && !tenant.isBlank()) return tenant;
        }
        return "DEFAULT";
    }

    private String resolveCorrelationId() {
        String mdc = MDC.get(CorrelationIdFilter.MDC_CORRELATION_ID);
        if (mdc != null && !mdc.isBlank()) return mdc;
        HttpServletRequest req = currentRequest();
        if (req != null) {
            String header = req.getHeader("X-Correlation-Id");
            if (header != null && !header.isBlank()) return header;
        }
        return "TR-" + System.currentTimeMillis();
    }

    private String clientIp() {
        HttpServletRequest req = currentRequest();
        if (req == null) return "127.0.0.1";
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr() != null ? req.getRemoteAddr() : "127.0.0.1";
    }

    private String userAgent() {
        HttpServletRequest req = currentRequest();
        if (req == null) return "Internal";
        String ua = req.getHeader("User-Agent");
        return ua != null ? ua : "Unknown";
    }

    private String deviceInfo(String ua) {
        if (ua == null) return "Unknown";
        if (ua.contains("Android")) return "Android Mobile";
        if (ua.contains("iPhone") || ua.contains("iPad")) return "iOS Mobile";
        if (ua.contains("Postman")) return "API Client (Postman)";
        if (ua.contains("Windows") || ua.contains("Macintosh") || ua.contains("Linux")) return "Desktop Web";
        return "Device (" + (ua.length() > 30 ? ua.substring(0, 30) : ua) + ")";
    }

    private String currentEndpoint() {
        HttpServletRequest req = currentRequest();
        if (req == null) return "-";
        return req.getMethod() + " " + req.getRequestURI();
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }
}
