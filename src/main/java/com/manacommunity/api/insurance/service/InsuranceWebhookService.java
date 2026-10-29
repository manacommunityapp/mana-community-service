package com.manacommunity.api.insurance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.insurance.entity.InsuranceEnums.PolicyStatus;
import com.manacommunity.api.insurance.entity.InsurancePartner;
import com.manacommunity.api.insurance.entity.PartnerWebhookEvent;
import com.manacommunity.api.insurance.partner.WebhookSignatureVerifier;
import com.manacommunity.api.insurance.partner.WebhookSignatureVerifier.Result;
import com.manacommunity.api.insurance.repository.InsurancePartnerRepository;
import com.manacommunity.api.insurance.repository.PartnerWebhookEventRepository;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Partner webhook ingestion. The endpoint is unauthenticated by session; trust comes ONLY from the HMAC.
 * Secrets come from {@code insurance.partners.<CODE>.webhook-secret} (env / secret manager). Fail-closed.
 * Raw bodies are not stored (only SHA-256) because they may contain personal data.
 */
@Service
public class InsuranceWebhookService {

    public static final long TOLERANCE_SECONDS = 300;

    public enum Outcome { PROCESSED, DUPLICATE, REJECTED_SIGNATURE, REJECTED_STALE, REJECTED_PAYLOAD, UNKNOWN_PARTNER }

    private final InsurancePartnerRepository partners;
    private final PartnerWebhookEventRepository events;
    private final InsurancePolicyService policyService;
    private final Environment env;
    private final ObjectMapper mapper = new ObjectMapper();

    public InsuranceWebhookService(InsurancePartnerRepository partners, PartnerWebhookEventRepository events,
                                   InsurancePolicyService policyService, Environment env) {
        this.partners = partners;
        this.events = events;
        this.policyService = policyService;
        this.env = env;
    }

    @Transactional
    public Outcome handle(String partnerCode, String signatureHeader, byte[] body) {
        String sha = sha256(body);
        InsurancePartner partner = partners.findByCode(partnerCode).filter(InsurancePartner::isSurfaceable).orElse(null);
        String secret = env.getProperty("insurance.partners." + partnerCode + ".webhook-secret");
        Result sig = partner == null ? Result.MISSING_SECRET
                : WebhookSignatureVerifier.verify(secret, signatureHeader, body, Instant.now().getEpochSecond(), TOLERANCE_SECONDS);
        if (sig != Result.VALID) {
            Outcome o = partner == null ? Outcome.UNKNOWN_PARTNER
                    : (sig == Result.STALE ? Outcome.REJECTED_STALE : Outcome.REJECTED_SIGNATURE);
            log(partner, partnerCode, null, false, sha, null, o);
            return o;
        }
        String eventId = null, type = null;
        try {
            JsonNode n = mapper.readTree(body);
            eventId = text(n, "eventId");
            type = text(n, "type");
            if (eventId == null || type == null) {
                log(partner, partnerCode, eventId, true, sha, type, Outcome.REJECTED_PAYLOAD);
                return Outcome.REJECTED_PAYLOAD;
            }
            if (events.existsByPartnerCodeAndEventIdAndOutcome(partnerCode, eventId, Outcome.PROCESSED.name())) {
                log(partner, partnerCode, eventId, true, sha, type, Outcome.DUPLICATE);
                return Outcome.DUPLICATE;
            }
            if ("POLICY_STATUS".equals(type)) {
                Long policyId = n.hasNonNull("policyId") ? n.get("policyId").asLong() : null;
                PolicyStatus status = parseStatus(text(n, "status"));
                if (policyId == null || status == null) {
                    log(partner, partnerCode, eventId, true, sha, type, Outcome.REJECTED_PAYLOAD);
                    return Outcome.REJECTED_PAYLOAD;
                }
                policyService.applyPartnerStatus(policyId, partner.getId(), eventId, status);
            }
            log(partner, partnerCode, eventId, true, sha, type, Outcome.PROCESSED);
            return Outcome.PROCESSED;
        } catch (Exception e) {
            log(partner, partnerCode, eventId, true, sha, type, Outcome.REJECTED_PAYLOAD);
            return Outcome.REJECTED_PAYLOAD;
        }
    }

    /** Partner-reported statuses we accept. A partner cannot set ACTIVE_DECLARED. */
    static PolicyStatus parseStatus(String s) {
        if (s == null) return null;
        return switch (s) {
            case "ACTIVE" -> PolicyStatus.ACTIVE_VERIFIED;
            case "EXPIRED" -> PolicyStatus.EXPIRED;
            case "LAPSED" -> PolicyStatus.LAPSED;
            case "CANCELLED" -> PolicyStatus.CANCELLED;
            default -> null;
        };
    }

    private void log(InsurancePartner partner, String code, String eventId, boolean sigValid, String sha,
                     String type, Outcome o) {
        events.save(PartnerWebhookEvent.builder()
                .partnerId(partner == null ? null : partner.getId())
                .partnerCode(code.length() > 40 ? code.substring(0, 40) : code)
                .eventId(eventId == null ? null : eventId.substring(0, Math.min(80, eventId.length())))
                .signatureValid(sigValid).payloadSha256(sha)
                .eventType(type == null ? null : type.substring(0, Math.min(40, type.length())))
                .outcome(o.name()).build());
    }

    private static String text(JsonNode n, String f) {
        return n.hasNonNull(f) ? n.get(f).asText() : null;
    }

    private static String sha256(byte[] b) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b == null ? new byte[0] : b));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
