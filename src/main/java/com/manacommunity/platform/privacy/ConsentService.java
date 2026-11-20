package com.manacommunity.platform.privacy;

import com.manacommunity.api.platform.web.CorrelationContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Append-only consent management per the DPDP Act §6.
 * Every grant and revoke creates a new record — no existing record is ever modified.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConsentService {

    private static final String CURRENT_NOTICE_VERSION = "v1.0";

    private final UserConsentRepository consentRepository;

    @Transactional
    public UserConsentRecord grantConsent(Long userId, Long communityId,
                                          ConsentPurposeCode purpose, Long actorId) {
        UserConsentRecord record = UserConsentRecord.builder()
                .userId(userId)
                .communityId(communityId)
                .purpose(purpose)
                .privacyNoticeVersion(CURRENT_NOTICE_VERSION)
                .action("GRANT")
                .actorId(actorId)
                .ipAddress(clientIp())
                .userAgent(userAgent())
                .build();
        UserConsentRecord saved = consentRepository.save(record);
        log.info("Consent GRANTED: user={} purpose={} notice={}", userId, purpose, CURRENT_NOTICE_VERSION);
        return saved;
    }

    @Transactional
    public UserConsentRecord revokeConsent(Long userId, Long communityId,
                                           ConsentPurposeCode purpose, Long actorId) {
        UserConsentRecord record = UserConsentRecord.builder()
                .userId(userId)
                .communityId(communityId)
                .purpose(purpose)
                .privacyNoticeVersion(CURRENT_NOTICE_VERSION)
                .action("REVOKE")
                .actorId(actorId)
                .ipAddress(clientIp())
                .userAgent(userAgent())
                .build();
        UserConsentRecord saved = consentRepository.save(record);
        log.info("Consent REVOKED: user={} purpose={}", userId, purpose);
        return saved;
    }

    public boolean hasActiveConsent(Long userId, ConsentPurposeCode purpose) {
        Optional<UserConsentRecord> latest = consentRepository.findCurrentConsent(userId, purpose);
        return latest.map(r -> "GRANT".equals(r.getAction())).orElse(false);
    }

    public List<UserConsentRecord> getConsentHistory(Long userId) {
        return consentRepository.findByUserIdOrderByCreatedAtDesc(userId);
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

    private String userAgent() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest().getHeader("User-Agent");
        } catch (Exception e) {
            return null;
        }
    }
}
