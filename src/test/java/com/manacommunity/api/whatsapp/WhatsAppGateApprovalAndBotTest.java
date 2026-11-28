package com.manacommunity.api.whatsapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.whatsapp.controller.WhatsAppWebhookController;
import com.manacommunity.api.whatsapp.model.WhatsAppOutboundMessage;
import com.manacommunity.api.whatsapp.service.GateApprovalStateMachine;
import com.manacommunity.api.whatsapp.service.GateApprovalStateMachine.*;
import com.manacommunity.api.whatsapp.service.WhatsAppConversationalBotEngine;
import com.manacommunity.api.whatsapp.service.WhatsAppConversationalBotEngine.*;
import com.manacommunity.api.whatsapp.service.WhatsAppIdempotencyService;
import com.manacommunity.api.whatsapp.service.WhatsAppSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WhatsAppGateApprovalAndBotTest {

    private static final String APP_SECRET = "test-meta-app-secret-64-character-token-value-12345678";
    private static final String VERIFY_TOKEN = "mana-mygate-webhook-token";

    private WhatsAppSignatureVerifier signatureVerifier;
    private WhatsAppIdempotencyService idempotencyService;
    private GateApprovalStateMachine gateApprovalStateMachine;
    private WhatsAppConversationalBotEngine conversationalBotEngine;
    private WhatsAppWebhookController controller;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        signatureVerifier = new WhatsAppSignatureVerifier(APP_SECRET, VERIFY_TOKEN);
        idempotencyService = new WhatsAppIdempotencyService();
        gateApprovalStateMachine = new GateApprovalStateMachine();
        conversationalBotEngine = new WhatsAppConversationalBotEngine();

        controller = new WhatsAppWebhookController(
                signatureVerifier,
                idempotencyService,
                gateApprovalStateMachine,
                conversationalBotEngine,
                objectMapper
        );
    }

    private String computeHmacSha256(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder("sha256=");
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Test
    void verifiesMetaChallengeGetRequestSuccessfully() {
        ResponseEntity<String> response = controller.verifyWebhook(
                "subscribe", VERIFY_TOKEN, "challenge_token_991823"
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("challenge_token_991823");

        ResponseEntity<String> invalid = controller.verifyWebhook(
                "subscribe", "wrong-token", "challenge_token_991823"
        );
        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void validatesHmacSha256SignatureAndRejectsTamperedPayload() throws Exception {
        String payload = "{\"object\":\"whatsapp_business_account\"}";
        String validSig = computeHmacSha256(payload, APP_SECRET);

        assertThat(signatureVerifier.verifySignature(payload, validSig)).isTrue();
        assertThat(signatureVerifier.verifySignature(payload + "tampered", validSig)).isFalse();
        assertThat(signatureVerifier.verifySignature(payload, "sha256=invalidhexsignature")).isFalse();
    }

    @Test
    void enforcesIdempotencyOnMetaAtLeastOnceWebhookRetries() throws Exception {
        String entryId = "ENTRY-MUM-7712";
        gateApprovalStateMachine.registerVisitorEntry(VisitorEntrySession.builder()
                .entryId(entryId)
                .societyId("SOC-001")
                .flatNumber("A-402")
                .visitorName("Swiggy Delivery - Rohan")
                .purpose("Food Delivery")
                .status(EntryStatus.PENDING)
                .build());

        String json = """
        {
          "object": "whatsapp_business_account",
          "entry": [{
            "id": "WABA-12345",
            "changes": [{
              "field": "messages",
              "value": {
                "messaging_product": "whatsapp",
                "messages": [{
                  "from": "919876543210",
                  "id": "wamid.HBgLMTEwMDk3OTk3NhUCMR",
                  "timestamp": "1732757400",
                  "type": "interactive",
                  "interactive": {
                    "type": "button_reply",
                    "button_reply": {
                      "id": "APPROVE:ENTRY-MUM-7712",
                      "title": "Approve"
                    }
                  }
                }]
              }
            }]
          }]
        }
        """;

        String sig = computeHmacSha256(json, APP_SECRET);

        // First delivery: processes and approves
        ResponseEntity<String> firstResp = controller.handleIncomingWebhook(sig, json);
        assertThat(firstResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        VisitorEntrySession entryAfterFirst = gateApprovalStateMachine.getEntry(entryId);
        assertThat(entryAfterFirst.getStatus()).isEqualTo(EntryStatus.APPROVED);

        // Second delivery (Meta network retry with identical wamid): acknowledged without re-executing
        ResponseEntity<String> secondResp = controller.handleIncomingWebhook(sig, json);
        assertThat(secondResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(idempotencyService.acquireLock("wamid.HBgLMTEwMDk3OTk3NhUCMR", null)).isFalse();
    }

    @Test
    void resolvesRaceConditionWhenWhatsAppApproveAndMobileAppDenyExecuteSimultaneously() {
        String entryId = "ENTRY-BANGALORE-9941";

        // Setup visitor waiting at gate
        gateApprovalStateMachine.registerVisitorEntry(VisitorEntrySession.builder()
                .entryId(entryId)
                .societyId("SOC-101")
                .flatNumber("B-1204")
                .visitorName("Zomato Delivery - Anand")
                .status(EntryStatus.PENDING)
                .build());

        // Co-resident 1 taps "DENY" on Mygate Mobile App first (milliseconds ahead)
        ApprovalResolutionResult mobileResult = gateApprovalStateMachine.submitDecision(
                entryId,
                "DENY",
                "Priya (Wife)",
                ApprovalChannel.MOBILE_APP
        );

        assertThat(mobileResult.isAccepted()).isTrue();
        assertThat(mobileResult.getFinalStatus()).isEqualTo(EntryStatus.DENIED);
        assertThat(mobileResult.getWinnerUser()).isEqualTo("Priya (Wife)");
        assertThat(mobileResult.getWinnerChannel()).isEqualTo(ApprovalChannel.MOBILE_APP);
        assertThat(mobileResult.isTriggerGateBarrierOpen()).isFalse();
        assertThat(mobileResult.isSuperseded()).isFalse();

        // Co-resident 2 simultaneously taps "APPROVE" on WhatsApp Quick-Reply
        ApprovalResolutionResult whatsappResult = gateApprovalStateMachine.submitDecision(
                entryId,
                "APPROVE",
                "Amit (Husband)",
                ApprovalChannel.WHATSAPP_QUICK_REPLY
        );

        // WhatsApp action is cleanly superseded!
        assertThat(whatsappResult.isAccepted()).isFalse();
        assertThat(whatsappResult.isSuperseded()).isTrue();
        assertThat(whatsappResult.getFinalStatus()).isEqualTo(EntryStatus.DENIED);
        assertThat(whatsappResult.getWinnerUser()).isEqualTo("Priya (Wife)");
        assertThat(whatsappResult.getWinnerChannel()).isEqualTo(ApprovalChannel.MOBILE_APP);
        assertThat(whatsappResult.isTriggerGateBarrierOpen()).isFalse();

        // Conflict notification message explains who won
        assertThat(whatsappResult.getResidentNotification())
                .contains("Action superseded")
                .contains("Zomato Delivery - Anand")
                .contains("DENIED")
                .contains("Priya (Wife)")
                .contains("Mygate Mobile App");
    }

    @Test
    void executesConversationalPayDuesBotFlowWithItemizedBreakdownAndDynamicUpiLink() {
        String phone = "919876543210";

        // Register resident profile
        conversationalBotEngine.registerResident(ResidentProfile.builder()
                .phoneNumber(phone)
                .residentName("Sunil Verma")
                .societyId("SOC-500")
                .societyName("Palm Springs RWA")
                .flatNumber("A-402")
                .vpaAddress("palmsprings.rwa@icici")
                .build());

        // Register unpaid maintenance invoice
        conversationalBotEngine.registerInvoice(InvoiceLedger.builder()
                .invoiceId("INV-2026-11-8841")
                .flatNumber("A-402")
                .totalAmount(5050.00)
                .itemizedLines(Map.of(
                        "Monthly Society Maintenance", 4500.00,
                        "Water Sub-metering", 350.00,
                        "Sinking Fund Contribution", 200.00
                ))
                .paid(false)
                .build());

        // User texts "Pay dues"
        WhatsAppOutboundMessage reply = conversationalBotEngine.processInboundMessage(
                phone, "Hi, please pay dues for this month", null
        );

        assertThat(reply.getTo()).isEqualTo(phone);
        assertThat(reply.getType()).isEqualTo("interactive");
        assertThat(reply.getInteractive()).isNotNull();

        String bodyText = reply.getInteractive().getBody().getText();
        assertThat(bodyText).contains("PALM SPRINGS RWA - Maintenance Dues");
        assertThat(bodyText).contains("Flat: *A-402*");
        assertThat(bodyText).contains("Monthly Society Maintenance: ₹4500.00");
        assertThat(bodyText).contains("Water Sub-metering: ₹350.00");
        assertThat(bodyText).contains("Sinking Fund Contribution: ₹200.00");
        assertThat(bodyText).contains("*Total Due: ₹5050.00*");

        // Verify Dynamic NPCI UPI URI string
        assertThat(bodyText).contains("upi://pay?pa=palmsprings.rwa@icici&pn=Palm+Springs+RWA&am=5050.00&tr=INV-2026-11-8841&cu=INR");

        // Session state must now be AWAITING_PAYMENT
        BotSession session = conversationalBotEngine.getSession(phone);
        assertThat(session).isNotNull();
        assertThat(session.getState()).isEqualTo(BotState.AWAITING_PAYMENT);
        assertThat(session.getActiveInvoiceId()).isEqualTo("INV-2026-11-8841");
        assertThat(session.getActiveDueAmount()).isEqualTo(5050.00);
    }

    @Test
    void dispatchesAutomatedPdfReceiptViaWhatsAppUponPaymentWebhookConfirmation() {
        String phone = "919876543210";
        String invoiceId = "INV-2026-11-8841";

        conversationalBotEngine.registerResident(ResidentProfile.builder()
                .phoneNumber(phone)
                .residentName("Sunil Verma")
                .societyId("SOC-500")
                .societyName("Palm Springs RWA")
                .flatNumber("A-402")
                .vpaAddress("palmsprings.rwa@icici")
                .build());

        conversationalBotEngine.registerInvoice(InvoiceLedger.builder()
                .invoiceId(invoiceId)
                .flatNumber("A-402")
                .totalAmount(5050.00)
                .itemizedLines(Map.of("Maintenance", 5050.00))
                .paid(false)
                .build());

        // Setup session in AWAITING_PAYMENT
        conversationalBotEngine.processInboundMessage(phone, "Pay dues", null);

        // Bank / Payment Gateway Webhook confirms payment
        String txnRef = "TXN-ICICI-UPI-99214451";
        String receiptPdfUrl = "https://cdn.mygate.com/receipts/2026/11/INV-2026-11-8841-RECEIPT.pdf";

        WhatsAppOutboundMessage receiptMessage = conversationalBotEngine.handlePaymentConfirmationWebhook(
                invoiceId,
                txnRef,
                5050.00,
                receiptPdfUrl
        );

        assertThat(receiptMessage).isNotNull();
        assertThat(receiptMessage.getTo()).isEqualTo(phone);
        assertThat(receiptMessage.getType()).isEqualTo("document");
        assertThat(receiptMessage.getDocument()).isNotNull();
        assertThat(receiptMessage.getDocument().getLink()).isEqualTo(receiptPdfUrl);
        assertThat(receiptMessage.getDocument().getFilename()).isEqualTo("Receipt_A-402_INV-2026-11-8841.pdf");
        assertThat(receiptMessage.getDocument().getCaption())
                .contains("Payment Received!")
                .contains("₹5050.00")
                .contains("TXN-ICICI-UPI-99214451");

        // Session state must now be PAYMENT_COMPLETED
        BotSession session = conversationalBotEngine.getSession(phone);
        assertThat(session.getState()).isEqualTo(BotState.PAYMENT_COMPLETED);
    }

    @Test
    void expiresConversationalSessionAfter45MinInactivityAndHandlesInvalidMidFlowTextGracefully() {
        Instant[] nowHolder = { Instant.parse("2026-11-28T09:00:00Z") };
        Clock mutableClock = new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return nowHolder[0]; }
        };

        WhatsAppConversationalBotEngine timedBot = new WhatsAppConversationalBotEngine(mutableClock);
        String phone = "919876543210";
        timedBot.registerResident(ResidentProfile.builder()
                .phoneNumber(phone)
                .residentName("Ramesh Nair")
                .societyId("SOC-500")
                .societyName("Palm Springs RWA")
                .flatNumber("C-101")
                .vpaAddress("palmsprings.rwa@icici")
                .build());
        timedBot.registerInvoice(InvoiceLedger.builder()
                .invoiceId("INV-2026-11-9001")
                .flatNumber("C-101")
                .totalAmount(3200.00)
                .itemizedLines(Map.of("Maintenance", 3200.00))
                .paid(false)
                .build());

        // 1. Resident starts Pay dues flow
        WhatsAppOutboundMessage startReply = timedBot.processInboundMessage(phone, "Pay dues", null);
        assertThat(startReply.getType()).isEqualTo("interactive");
        assertThat(timedBot.getSession(phone).getState()).isEqualTo(BotState.AWAITING_PAYMENT);

        // 2. Unrecognized gibberish mid-flow never throws NPE and keeps AWAITING_PAYMENT active
        WhatsAppOutboundMessage invalidReply = timedBot.processInboundMessage(phone, "asdf", null);
        assertThat(invalidReply.getType()).isEqualTo("text");
        assertThat(invalidReply.getText().getBody()).contains("didn't understand").contains("INV-2026-11-9001");
        assertThat(timedBot.getSession(phone).getState()).isEqualTo(BotState.AWAITING_PAYMENT);

        // 3. Advance clock by 45 minutes (> 30 min TTL) and resident types "Confirm"
        nowHolder[0] = nowHolder[0].plus(Duration.ofMinutes(45));
        WhatsAppOutboundMessage expiredReply = timedBot.processInboundMessage(phone, "Confirm", null);
        assertThat(expiredReply.getType()).isEqualTo("text");
        assertThat(expiredReply.getText().getBody()).contains("expired").contains("Pay dues");
        assertThat(timedBot.getSession(phone).getState()).isEqualTo(BotState.IDLE);

        // 4. Typing "Pay dues" recovers into a fresh AWAITING_PAYMENT session
        WhatsAppOutboundMessage recovered = timedBot.processInboundMessage(phone, "Pay dues", null);
        assertThat(recovered.getType()).isEqualTo("interactive");
        assertThat(timedBot.getSession(phone).getState()).isEqualTo(BotState.AWAITING_PAYMENT);
    }
}
