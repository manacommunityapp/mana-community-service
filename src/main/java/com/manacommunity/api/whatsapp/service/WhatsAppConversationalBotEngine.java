package com.manacommunity.api.whatsapp.service;

import com.manacommunity.api.whatsapp.model.WhatsAppOutboundMessage;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stateless / Redis-backed conversational bot engine over Meta WhatsApp Cloud API.
 * Orchestrates resident intent detection ('Pay dues'), itemized maintenance ledger display,
 * dynamic UPI intent generation, 30-minute session TTL expiry/recovery, and automated PDF
 * receipt dispatch upon payment webhook.
 */
@Service
@Slf4j
public class WhatsAppConversationalBotEngine {

    public static final Duration SESSION_TTL = Duration.ofMinutes(30);

    public enum BotState { IDLE, AWAITING_PAYMENT, PAYMENT_COMPLETED }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResidentProfile {
        private String phoneNumber;
        private String residentName;
        private String societyId;
        private String societyName;
        private String flatNumber;
        private String vpaAddress; // society UPI VPA
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InvoiceLedger {
        private String invoiceId;
        private String flatNumber;
        private double totalAmount;
        private Map<String, Double> itemizedLines;
        private boolean paid;
        private String transactionRef;
        private String receiptPdfUrl;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BotSession {
        private String phoneNumber;
        private BotState state;
        private String activeInvoiceId;
        private double activeDueAmount;
        private Instant lastActivity;
    }

    private final Clock clock;

    // In production, backed by Redis: key = "wa:bot:session:{phone}"
    private final Map<String, BotSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, ResidentProfile> residentRegistry = new ConcurrentHashMap<>();
    private final Map<String, InvoiceLedger> invoiceRegistry = new ConcurrentHashMap<>();

    public WhatsAppConversationalBotEngine() {
        this(Clock.systemUTC());
    }

    public WhatsAppConversationalBotEngine(Clock clock) {
        this.clock = clock;
    }

    public void registerResident(ResidentProfile profile) {
        residentRegistry.put(normalizePhone(profile.getPhoneNumber()), profile);
    }

    public void registerInvoice(InvoiceLedger invoice) {
        invoiceRegistry.put(invoice.getInvoiceId(), invoice);
    }

    public BotSession getSession(String phoneNumber) {
        return sessions.get(normalizePhone(phoneNumber));
    }

    /**
     * Handles inbound user text or interactive button clicks.
     */
    public WhatsAppOutboundMessage processInboundMessage(String senderPhone, String messageBody, String buttonPayload) {
        String phone = normalizePhone(senderPhone);
        ResidentProfile resident = residentRegistry.get(phone);

        if (resident == null) {
            return WhatsAppOutboundMessage.builder()
                    .to(phone)
                    .type("text")
                    .text(WhatsAppOutboundMessage.Text.builder()
                            .body("Welcome to Mana Community. Your mobile number is not registered with any society flat. Please contact your society manager.")
                            .build())
                    .build();
        }

        String intentText = (buttonPayload != null && !buttonPayload.isBlank()) ?
                buttonPayload : (messageBody != null ? messageBody.trim().toLowerCase(Locale.ROOT) : "");

        Instant now = clock.instant();
        BotSession session = sessions.computeIfAbsent(phone, p -> BotSession.builder()
                .phoneNumber(p)
                .state(BotState.IDLE)
                .lastActivity(now)
                .build());

        // Enforce 30-minute inactivity TTL on active multi-step bot sessions
        if (session.getState() != BotState.IDLE
                && session.getLastActivity() != null
                && Duration.between(session.getLastActivity(), now).compareTo(SESSION_TTL) > 0) {
            log.info("WhatsApp bot session expired for phone [{}] after {} min",
                    phone, Duration.between(session.getLastActivity(), now).toMinutes());
            session.setState(BotState.IDLE);
            session.setActiveInvoiceId(null);
            session.setActiveDueAmount(0);
            session.setLastActivity(now);

            // Unless user explicitly starts a fresh 'Pay dues' flow, ask them to restart cleanly
            if (!(intentText.contains("pay") || intentText.contains("due") || intentText.contains("bill") || intentText.contains("maintenance"))) {
                return textReply(phone,
                        "Your session expired due to inactivity. Type *Pay dues* to start again.");
            }
        }

        // Intent detection: "pay dues", "view bill", "maintenance"
        if (intentText.contains("pay") || intentText.contains("due") || intentText.contains("bill") || intentText.contains("maintenance")) {
            return handlePayDuesIntent(resident, session);
        }

        if ("cancel".equals(intentText) || "reset".equals(intentText)) {
            session.setState(BotState.IDLE);
            session.setActiveInvoiceId(null);
            session.setActiveDueAmount(0);
            session.setLastActivity(now);
            return textReply(phone, "Session cleared. Reply *Pay dues* whenever you are ready.");
        }

        if (session.getState() == BotState.AWAITING_PAYMENT) {
            session.setLastActivity(now);
            if (intentText.contains("confirm") || intentText.startsWith("paid_check:")) {
                return textReply(phone, String.format(
                        "Invoice `%s` (₹%.2f) is awaiting bank UPI confirmation. Once settled, your PDF receipt will arrive here automatically.",
                        session.getActiveInvoiceId(), session.getActiveDueAmount()));
            }
            // Graceful handling of unrecognized mid-flow text without throwing NPE
            return textReply(phone, String.format(
                    "I didn't understand \"%s\". Please complete the UPI payment for `%s` (₹%.2f) or reply *cancel* to exit.",
                    messageBody != null ? messageBody.trim() : "",
                    session.getActiveInvoiceId(), session.getActiveDueAmount()));
        }

        session.setLastActivity(now);
        // Default menu
        return textReply(phone, String.format(
                "Hello %s (Flat %s, %s)!\nReply *Pay dues* to view your pending maintenance balance and pay via UPI.",
                resident.getResidentName(), resident.getFlatNumber(), resident.getSocietyName()));
    }

    private WhatsAppOutboundMessage textReply(String to, String body) {
        return WhatsAppOutboundMessage.builder()
                .to(to)
                .type("text")
                .text(WhatsAppOutboundMessage.Text.builder().body(body).build())
                .build();
    }

    private WhatsAppOutboundMessage handlePayDuesIntent(ResidentProfile resident, BotSession session) {
        // Find unpaid invoice for flat
        InvoiceLedger invoice = invoiceRegistry.values().stream()
                .filter(inv -> inv.getFlatNumber().equalsIgnoreCase(resident.getFlatNumber()) && !inv.isPaid())
                .findFirst()
                .orElse(null);

        if (invoice == null) {
            session.setState(BotState.IDLE);
            return WhatsAppOutboundMessage.builder()
                    .to(resident.getPhoneNumber())
                    .type("text")
                    .text(WhatsAppOutboundMessage.Text.builder()
                            .body(String.format("Great news %s! Flat %s has *NO pending maintenance dues* at %s.",
                                    resident.getResidentName(), resident.getFlatNumber(), resident.getSocietyName()))
                            .build())
                    .build();
        }

        // Build dynamic UPI intent string
        String upiIntent = generateUpiIntentLink(
                resident.getVpaAddress(),
                resident.getSocietyName(),
                invoice.getTotalAmount(),
                invoice.getInvoiceId()
        );

        // Update session state
        session.setState(BotState.AWAITING_PAYMENT);
        session.setActiveInvoiceId(invoice.getInvoiceId());
        session.setActiveDueAmount(invoice.getTotalAmount());
        session.setLastActivity(clock.instant());

        // Construct itemized message body
        StringBuilder body = new StringBuilder();
        body.append(String.format("*%s - Maintenance Dues*\n", resident.getSocietyName().toUpperCase()));
        body.append(String.format("Flat: *%s* | Resident: *%s*\n", resident.getFlatNumber(), resident.getResidentName()));
        body.append(String.format("Invoice No: `%s`\n\n", invoice.getInvoiceId()));
        body.append("*Itemized Breakdown:*\n");

        invoice.getItemizedLines().forEach((item, amt) ->
                body.append(String.format("• %s: ₹%.2f\n", item, amt))
        );

        body.append(String.format("\n*Total Due: ₹%.2f*\n\n", invoice.getTotalAmount()));
        body.append("Click the UPI payment link below to pay directly via GPay, PhonePe, Paytm, or BHIM:\n");
        body.append(upiIntent);

        return WhatsAppOutboundMessage.builder()
                .to(resident.getPhoneNumber())
                .type("interactive")
                .interactive(WhatsAppOutboundMessage.Interactive.builder()
                        .type("button")
                        .header(WhatsAppOutboundMessage.Header.builder()
                                .type("text")
                                .text("Maintenance Dues Notice")
                                .build())
                        .body(WhatsAppOutboundMessage.Body.builder()
                                .text(body.toString())
                                .build())
                        .footer(WhatsAppOutboundMessage.Footer.builder()
                                .text("Mana Community Official Billing Bot")
                                .build())
                        .action(WhatsAppOutboundMessage.Action.builder()
                                .buttons(List.of(
                                        WhatsAppOutboundMessage.ButtonWrapper.builder()
                                                .type("reply")
                                                .reply(WhatsAppOutboundMessage.Reply.builder()
                                                        .id("PAID_CHECK:" + invoice.getInvoiceId())
                                                        .title("I've Paid")
                                                        .build())
                                                .build(),
                                        WhatsAppOutboundMessage.ButtonWrapper.builder()
                                                .type("reply")
                                                .reply(WhatsAppOutboundMessage.Reply.builder()
                                                        .id("HELP_DESK:" + invoice.getInvoiceId())
                                                        .title("Query Dues")
                                                        .build())
                                                .build()
                                ))
                                .build())
                        .build())
                .build();
    }

    /**
     * Triggered by payment gateway or bank webhook when UPI payment confirms.
     * Marks invoice paid, transitions bot session, and emits automated PDF receipt outbound message.
     */
    public WhatsAppOutboundMessage handlePaymentConfirmationWebhook(
            String invoiceId,
            String transactionRef,
            double amountPaid,
            String receiptCdnUrl) {

        InvoiceLedger invoice = invoiceRegistry.get(invoiceId);
        if (invoice == null) {
            log.error("Payment webhook received for non-existent invoice [{}]", invoiceId);
            return null;
        }

        invoice.setPaid(true);
        invoice.setTransactionRef(transactionRef);
        invoice.setReceiptPdfUrl(receiptCdnUrl);

        // Find resident
        ResidentProfile resident = residentRegistry.values().stream()
                .filter(r -> r.getFlatNumber().equalsIgnoreCase(invoice.getFlatNumber()))
                .findFirst()
                .orElse(null);

        if (resident == null) {
            log.warn("Payment confirmed for invoice [{}] but no resident mapped to flat [{}]",
                    invoiceId, invoice.getFlatNumber());
            return null;
        }

        // Update session
        BotSession session = sessions.get(normalizePhone(resident.getPhoneNumber()));
        if (session != null) {
            session.setState(BotState.PAYMENT_COMPLETED);
            session.setLastActivity(clock.instant());
        }

        String caption = String.format(
                "Payment Received! ₹%.2f credited to %s.\nTxn Ref: `%s`\nYour official maintenance receipt is attached.",
                amountPaid, resident.getSocietyName(), transactionRef
        );

        String filename = String.format("Receipt_%s_%s.pdf", resident.getFlatNumber(), invoiceId);

        log.info("Dispatched automated PDF receipt for invoice [{}] to [{}] via WhatsApp",
                invoiceId, resident.getPhoneNumber());

        return WhatsAppOutboundMessage.builder()
                .to(resident.getPhoneNumber())
                .type("document")
                .document(WhatsAppOutboundMessage.Document.builder()
                        .link(receiptCdnUrl)
                        .caption(caption)
                        .filename(filename)
                        .build())
                .build();
    }

    public static String generateUpiIntentLink(String vpa, String payeeName, double amount, String invoiceRef) {
        String encodedPayee = URLEncoder.encode(payeeName, StandardCharsets.UTF_8);
        return String.format(Locale.US,
                "upi://pay?pa=%s&pn=%s&am=%.2f&tr=%s&cu=INR",
                vpa, encodedPayee, amount, invoiceRef);
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }

    public void clear() {
        sessions.clear();
        residentRegistry.clear();
        invoiceRegistry.clear();
    }
}
