package com.manacommunity.api.whatsapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.whatsapp.model.WhatsAppOutboundMessage;
import com.manacommunity.api.whatsapp.model.WhatsAppWebhookPayload;
import com.manacommunity.api.whatsapp.service.GateApprovalStateMachine;
import com.manacommunity.api.whatsapp.service.GateApprovalStateMachine.ApprovalChannel;
import com.manacommunity.api.whatsapp.service.GateApprovalStateMachine.ApprovalResolutionResult;
import com.manacommunity.api.whatsapp.service.WhatsAppConversationalBotEngine;
import com.manacommunity.api.whatsapp.service.WhatsAppIdempotencyService;
import com.manacommunity.api.whatsapp.service.WhatsAppSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * Ingestion controller for Meta WhatsApp Cloud API webhooks.
 * Implements HMAC signature checking, idempotency deduplication,
 * and routes to either Gate Approval state machine or Conversational Billing Bot.
 */
@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final WhatsAppSignatureVerifier signatureVerifier;
    private final WhatsAppIdempotencyService idempotencyService;
    private final GateApprovalStateMachine gateApprovalStateMachine;
    private final WhatsAppConversationalBotEngine conversationalBotEngine;
    private final ObjectMapper objectMapper;

    /**
     * Webhook verification challenge from Meta.
     */
    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if (signatureVerifier.verifyChallenge(mode, token)) {
            log.info("Meta WhatsApp webhook challenge verified successfully.");
            return ResponseEntity.ok(challenge);
        }
        log.warn("Meta WhatsApp webhook challenge verification failed. Token mismatch.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification token mismatch");
    }

    /**
     * Inbound event webhook handler.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleIncomingWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String rawPayload) {

        // 1. Signature Verification
        if (!signatureVerifier.verifySignature(rawPayload, signature)) {
            log.error("Rejecting WhatsApp webhook: Invalid HMAC-SHA256 signature.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        try {
            WhatsAppWebhookPayload payload = objectMapper.readValue(rawPayload, WhatsAppWebhookPayload.class);
            if (payload == null || payload.getEntry() == null) {
                return ResponseEntity.ok("EVENT_RECEIVED");
            }

            for (WhatsAppWebhookPayload.Entry entry : payload.getEntry()) {
                if (entry.getChanges() == null) continue;

                for (WhatsAppWebhookPayload.Change change : entry.getChanges()) {
                    if (change.getValue() == null || change.getValue().getMessages() == null) continue;

                    for (WhatsAppWebhookPayload.Message msg : change.getValue().getMessages()) {
                        processMessage(msg);
                    }
                }
            }

            // Always return 200 OK within 3s to satisfy Meta Cloud API SLA
            return ResponseEntity.ok("EVENT_RECEIVED");
        } catch (Exception ex) {
            log.error("Error processing WhatsApp webhook payload: {}", ex.getMessage(), ex);
            return ResponseEntity.ok("EVENT_RECEIVED"); // Acknowledge to prevent retry loops on malformed JSON
        }
    }

    private void processMessage(WhatsAppWebhookPayload.Message msg) {
        String messageId = msg.getId();

        // 2. Meta at-least-once deduplication check
        if (!idempotencyService.acquireLock(messageId, Duration.ofHours(24))) {
            log.info("Ignoring duplicate WhatsApp webhook message [{}]", messageId);
            return;
        }

        String senderPhone = msg.getFrom();

        // Check if interactive button reply
        if ("interactive".equals(msg.getType()) && msg.getInteractive() != null &&
                msg.getInteractive().getButtonReply() != null) {

            String buttonId = msg.getInteractive().getButtonReply().getId();
            log.info("Received WhatsApp interactive button reply: [{}] from [{}]", buttonId, senderPhone);

            // Gate Visitor Approval Flow: "APPROVE:{entryId}" or "DENY:{entryId}"
            if (buttonId.startsWith("APPROVE:") || buttonId.startsWith("DENY:")) {
                String[] parts = buttonId.split(":", 2);
                String decision = parts[0];
                String entryId = parts[1];

                ApprovalResolutionResult result = gateApprovalStateMachine.submitDecision(
                        entryId,
                        decision,
                        senderPhone,
                        ApprovalChannel.WHATSAPP_QUICK_REPLY
                );
                idempotencyService.cacheResult(messageId, result);
                return;
            }

            // Conversational Bot button reply (e.g., "PAID_CHECK:{invId}")
            WhatsAppOutboundMessage botReply = conversationalBotEngine.processInboundMessage(
                    senderPhone, null, buttonId
            );
            idempotencyService.cacheResult(messageId, botReply);
            return;
        }

        // Standard text message routing
        if (msg.getText() != null) {
            String text = msg.getText().getBody();
            WhatsAppOutboundMessage botReply = conversationalBotEngine.processInboundMessage(
                    senderPhone, text, null
            );
            idempotencyService.cacheResult(messageId, botReply);
        }
    }
}
