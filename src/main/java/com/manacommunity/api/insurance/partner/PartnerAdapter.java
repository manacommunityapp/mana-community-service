package com.manacommunity.api.insurance.partner;

import com.manacommunity.api.insurance.entity.InsuranceEnums;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Provider-neutral adapter for an insurer / IRDAI-authorised intermediary.
 *
 * <p>Contract rules:
 * <ul>
 *   <li>An adapter must only return data the partner actually returned. It must never synthesise a quote,
 *       payment result, issued policy, claim status or approval. Failure = {@link PartnerException}.</li>
 *   <li>Sandbox adapters must report {@link #isSandbox()} {@code true}; their output is persisted and shown
 *       with a SANDBOX label.</li>
 *   <li>Operations a partner does not support keep the default {@code NOT_SUPPORTED} behaviour.</li>
 * </ul>
 * Credential storage, request signing and TLS configuration live inside concrete adapters and are sourced
 * from environment/secret store (see docs/insurance/env-vars.md), never from the database or source.
 */
public interface PartnerAdapter {

    String partnerCode();

    boolean isSandbox();

    /** Quote request. {@code input.fields()} is held in memory only and never persisted by Mana. */
    QuoteOffer requestQuote(QuoteInput input) throws PartnerException;

    /** Refresh an expired/stale quote. */
    default QuoteOffer refreshQuote(String partnerQuoteRef, String traceId) throws PartnerException {
        throw PartnerException.notSupported("refreshQuote");
    }

    /** Begin a proposal; returns a partner-hosted URL the user is handed off to. */
    default HandoffLink initiateProposal(String partnerQuoteRef, String traceId) throws PartnerException {
        throw PartnerException.notSupported("initiateProposal");
    }

    /** Partner-reported issuance status for a proposal. Only partner-confirmed values may be returned. */
    default String policyIssuanceStatus(String partnerApplicationRef, String traceId) throws PartnerException {
        throw PartnerException.notSupported("policyIssuanceStatus");
    }

    /** Partner claim-support reference/contact (not a claim decision). */
    default String claimsSupportReference(String partnerPolicyRef, String traceId) throws PartnerException {
        throw PartnerException.notSupported("claimsSupportReference");
    }

    record QuoteInput(InsuranceEnums.Category category, String idempotencyKey, String traceId,
                      Map<String, Object> fields) {}

    record QuoteOffer(String partnerQuoteRef, BigDecimal premium, String frequency, BigDecimal sumInsured,
                      LocalDateTime validUntil) {}

    record HandoffLink(String url) {}
}
