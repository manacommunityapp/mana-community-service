package com.manacommunity.api.email.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.email.EmailMessage;
import com.manacommunity.api.email.EmailService;
import com.manacommunity.platform.sqs.SqsEventEnvelope;
import com.manacommunity.platform.sqs.SqsMessageHandler;
import com.manacommunity.platform.sqs.SqsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SQS consumer for {@code mana-email-dispatch}.
 *
 * <p>Decouples transactional email sending (registration confirmations, approval/rejection
 * notices, payment receipts, sports match results) from the HTTP request thread. This prevents
 * Gmail / AWS SES SMTP round-trip latency (typically 1–3 s) from blocking API responses.
 *
 * <p>All event types routed through this queue carry a fully-rendered {@link EmailMessage}
 * payload so the consumer needs no further template processing.
 *
 * <p>Supported {@code eventType} values:
 * <ul>
 *   <li>{@code REGISTRATION_EMAIL}    — sports / event registration lifecycle (RECEIVED, CONFIRMED, REJECTED)</li>
 *   <li>{@code PAYMENT_RECEIPT}       — payment success / failure receipts</li>
 *   <li>{@code GENERIC_EMAIL}         — catch-all for ad-hoc transactional emails</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailDispatchSqsHandler implements SqsMessageHandler {

    private final EmailService emailService;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    /**
     * Fully-rendered email payload stored in SQS.
     * Fields mirror {@link EmailMessage} to allow direct deserialization.
     */
    public record EmailDispatchPayload(
            String to,
            String toName,
            String subject,
            String htmlBody,
            String from,
            String fromName
    ) {
        /** Convenience constructor — omits optional sender override fields. */
        public EmailDispatchPayload(String to, String toName, String subject, String htmlBody) {
            this(to, toName, subject, htmlBody, null, null);
        }
    }

    @Override
    public String queueName() {
        return sqsProperties.getQueues().getEmailDispatch();
    }

    @Override
    public void handle(SqsEventEnvelope envelope) throws Exception {
        EmailDispatchPayload payload = objectMapper.readValue(
                envelope.payloadJson(), EmailDispatchPayload.class);
        deliverEmail(payload, envelope.eventType(), envelope.eventId());
    }

    /**
     * Dispatches the email directly (called both by the SQS consumer and as the
     * in-process fallback when SQS is disabled).
     */
    public void deliverEmail(EmailDispatchPayload payload, String eventType, String eventId) {
        if (payload == null || payload.to() == null || payload.to().isBlank()) {
            log.warn("[EMAIL] Skipping dispatch — missing recipient. eventType={} eventId={}", eventType, eventId);
            return;
        }
        try {
            EmailMessage message = EmailMessage.builder()
                    .to(payload.to())
                    .toName(payload.toName())
                    .subject(payload.subject())
                    .htmlBody(payload.htmlBody())
                    .from(payload.from())
                    .fromName(payload.fromName())
                    .build();
            emailService.send(message);
            log.debug("[EMAIL] Dispatched eventType={} to={} eventId={}", eventType, payload.to(), eventId);
        } catch (Exception ex) {
            log.error("[EMAIL] Dispatch failed for eventType={} to={} eventId={}: {}",
                    eventType, payload.to(), eventId, ex.getMessage(), ex);
            throw ex; // Re-throw so SQS returns the message to the queue for retry
        }
    }
}
