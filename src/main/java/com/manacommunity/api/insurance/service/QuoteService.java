package com.manacommunity.api.insurance.service;

import com.manacommunity.api.exception.ManaCommunityException;
import com.manacommunity.api.insurance.dto.InsuranceDtos.*;
import com.manacommunity.api.insurance.entity.*;
import com.manacommunity.api.insurance.entity.InsuranceEnums.*;
import com.manacommunity.api.insurance.partner.PartnerAdapter;
import com.manacommunity.api.insurance.partner.PartnerCallExecutor;
import com.manacommunity.api.insurance.partner.PartnerException;
import com.manacommunity.api.insurance.repository.*;
import com.manacommunity.api.user.model.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Quote orchestration. Rules enforced here:
 * - active QUOTE_SHARING consent is required before any data leaves Mana
 * - idempotent per (user, Idempotency-Key); same key + different payload = 409
 * - inputs are validated against an allow-list and are NOT persisted (only a hash)
 * - when no adapter/partner is available we record PARTNER_UNAVAILABLE and return NO quotes (never fabricated)
 */
@Service
public class QuoteService {

    private static final Logger log = LoggerFactory.getLogger(QuoteService.class);

    private final QuoteRequestRepository requests;
    private final InsuranceQuoteRepository quotes;
    private final InsurancePartnerRepository partners;
    private final InsuranceConsentService consents;
    private final InsuranceAuditService audit;
    private final Map<String, PartnerAdapter> adapters;
    private final PartnerCallExecutor executor;

    public QuoteService(QuoteRequestRepository requests, InsuranceQuoteRepository quotes,
                        InsurancePartnerRepository partners, InsuranceConsentService consents,
                        InsuranceAuditService audit, List<PartnerAdapter> adapterList,
                        @Value("${insurance.partner.max-attempts:3}") int maxAttempts,
                        @Value("${insurance.partner.backoff-ms:300}") long backoffMs,
                        @Value("${insurance.partner.timeout-ms:8000}") long timeoutMs) {
        this.requests = requests;
        this.quotes = quotes;
        this.partners = partners;
        this.consents = consents;
        this.audit = audit;
        this.adapters = adapterList.stream().collect(Collectors.toMap(PartnerAdapter::partnerCode, Function.identity()));
        this.executor = new PartnerCallExecutor(maxAttempts, backoffMs, timeoutMs);
    }

    @Transactional
    public QuoteRequestView submit(AppUser user, String idempotencyKey, QuoteSubmitRequest req) {
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 80) {
            throw new ManaCommunityException("Idempotency-Key header is required", HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED");
        }
        Map<String, String> errors = QuoteFieldValidator.validate(req.category(), req.fields(), LocalDate.now());
        if (!errors.isEmpty()) {
            // Field names only — never echo values (may be sensitive).
            throw new ManaCommunityException("Invalid fields: " + String.join(", ", errors.keySet()),
                    HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        }
        String hash = hash(req);
        Optional<QuoteRequest> existing = requests.findByUserIdAndIdempotencyKey(user.getId(), idempotencyKey);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(hash)) {
                throw new ManaCommunityException("Idempotency-Key was already used with a different request",
                        HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED");
            }
            return view(existing.get(), true);
        }

        ConsentRecord consent = consents.requireActive(user.getId(), req.consentId(), ConsentPurpose.QUOTE_SHARING);

        PartnerAdapter adapter = adapters.get(req.partnerCode());
        InsurancePartner partner = partners.findByCode(req.partnerCode()).orElse(null);
        boolean usable = adapter != null && (adapter.isSandbox() || (partner != null && partner.isSurfaceable()));
        if (!adapter_consentMatches(consent, partner, adapter)) {
            throw new ManaCommunityException("Consent does not cover this partner", HttpStatus.FORBIDDEN, "CONSENT_REQUIRED");
        }

        QuoteRequest qr = QuoteRequest.builder()
                .userId(user.getId())
                .communityId(user.getCommunity() == null ? null : user.getCommunity().getId())
                .category(req.category()).partnerId(partner == null ? null : partner.getId())
                .consentId(consent.getId()).idempotencyKey(idempotencyKey).requestHash(hash)
                .status(QuoteRequestStatus.SUBMITTED).build();

        String traceId = UUID.randomUUID().toString();
        if (!usable) {
            qr.setStatus(QuoteRequestStatus.PARTNER_UNAVAILABLE);
            qr.setFailureCode("NO_AUTHORISED_PARTNER");
            qr = requests.save(qr);
            audit.record(user.getId(), "QUOTE_REQUEST_UNAVAILABLE", "QUOTE_REQUEST", String.valueOf(qr.getId()), "traceId=" + traceId);
            return view(qr, false);
        }
        qr = requests.save(qr);
        try {
            PartnerAdapter.QuoteOffer offer = executor.call(() -> adapter.requestQuote(
                    new PartnerAdapter.QuoteInput(req.category(), idempotencyKey, traceId, req.fields())));
            quotes.save(InsuranceQuote.builder()
                    .quoteRequestId(qr.getId()).partnerId(partner == null ? null : partner.getId())
                    .partnerQuoteRef(offer.partnerQuoteRef())
                    .dataSource(adapter.isSandbox() ? QuoteSource.SANDBOX : QuoteSource.PARTNER)
                    .premiumAmount(offer.premium()).premiumFrequency(offer.frequency()).sumInsured(offer.sumInsured())
                    .quotedAt(LocalDateTime.now()).validUntil(offer.validUntil()).build());
            qr.setStatus(QuoteRequestStatus.QUOTED);
        } catch (PartnerException e) {
            qr.setStatus(QuoteRequestStatus.FAILED);
            qr.setFailureCode(e.getCode());
            log.warn("insurance quote failed partner={} code={} traceId={}", req.partnerCode(), e.getCode(), traceId);
        }
        qr = requests.save(qr);
        audit.record(user.getId(), "QUOTE_REQUESTED", "QUOTE_REQUEST", String.valueOf(qr.getId()),
                "status=" + qr.getStatus() + " traceId=" + traceId);
        return view(qr, false);
    }

    private static boolean adapter_consentMatches(ConsentRecord c, InsurancePartner partner, PartnerAdapter adapter) {
        // Consent with no partner scope covers any partner; scoped consent must match the chosen partner.
        if (c.getPartnerId() == null) return true;
        return partner != null && c.getPartnerId().equals(partner.getId());
    }

    public QuoteRequestView get(Long userId, Long id) {
        QuoteRequest qr = requests.findById(id).filter(r -> r.getUserId().equals(userId))
                .orElseThrow(() -> new ManaCommunityException("Quote request not found", HttpStatus.NOT_FOUND, "NOT_FOUND"));
        return view(qr, false);
    }

    private QuoteRequestView view(QuoteRequest qr, boolean replayed) {
        LocalDateTime now = LocalDateTime.now();
        List<QuoteView> qv = quotes.findByQuoteRequestId(qr.getId()).stream().map(q -> {
            String partnerName = q.getPartnerId() == null ? "Sandbox (sample)"
                    : partners.findById(q.getPartnerId()).map(InsurancePartner::getLegalName).orElse("Partner");
            boolean sandbox = q.getDataSource() == QuoteSource.SANDBOX;
            return new QuoteView(q.getId(), partnerName, q.getDataSource().name(), q.getPremiumAmount(),
                    q.getPremiumFrequency(), q.getSumInsured(), q.getQuotedAt(), q.getValidUntil(), q.isExpired(now),
                    sandbox ? "SAMPLE DATA - not a real quote" : null);
        }).toList();
        String notice = qr.getStatus() == QuoteRequestStatus.PARTNER_UNAVAILABLE
                ? "No authorised insurance partner is currently available for this request. No quote was generated."
                : (qr.getStatus() == QuoteRequestStatus.FAILED
                ? "The partner could not return a quote. You can retry later. No quote was generated." : null);
        return new QuoteRequestView(qr.getId(), qr.getCategory().name(), qr.getStatus().name(), qr.getFailureCode(),
                replayed, qv, notice);
    }

    private static String hash(QuoteSubmitRequest r) {
        String canonical = r.category() + "|" + r.partnerCode() + "|" + r.consentId() + "|" + new TreeMap<>(r.fields());
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
