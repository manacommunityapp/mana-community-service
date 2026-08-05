package com.manacommunity.api.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Default {@link EmailService} backed by Spring's {@link JavaMailSender}.
 *
 * <p>Delivery is gated by {@code app.mail.enabled}. When disabled — or when no
 * {@code JavaMailSender} bean is available because no SMTP host is configured —
 * the message is fully rendered and logged but not sent. This keeps local/dev
 * environments safe (no accidental mail) while production simply flips the flag,
 * exactly like {@code PushNotificationServiceStub} for push.</p>
 */
@Slf4j
@Service
public class SmtpEmailService implements EmailService {

    /**
     * Callers can set these MDC keys before calling {@code send()} to tag the
     * delivery log entry with the template type and community — without changing
     * the {@link EmailMessage} record signature.
     * Example: {@code MDC.put(SmtpEmailService.MDC_TEMPLATE_TYPE, "OTP");}
     */
    public static final String MDC_TEMPLATE_TYPE  = "emailTemplateType";
    public static final String MDC_COMMUNITY_ID   = "emailCommunityId";

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailProperties props;
    private final EmailDeliveryLogRepository deliveryLogRepo;

    public SmtpEmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                             EmailProperties props,
                             EmailDeliveryLogRepository deliveryLogRepo) {
        this.mailSenderProvider = mailSenderProvider;
        this.props = props;
        this.deliveryLogRepo = deliveryLogRepo;
    }

    @Override
    public void send(EmailMessage message) {
        if (message == null || message.to() == null || message.to().isBlank()) {
            log.debug("[EMAIL] Skipped — no recipient address");
            return;
        }

        // Read optional context tags set by callers via MDC
        String templateType = MDC.get(MDC_TEMPLATE_TYPE);
        Long communityId    = parseCommunityId(MDC.get(MDC_COMMUNITY_ID));

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (!props.isEnabled() || sender == null) {
            log.info("[EMAIL DISABLED] Would send '{}' to {} ({} chars of HTML)",
                    message.subject(), message.to(),
                    message.htmlBody() == null ? 0 : message.htmlBody().length());
            persistSafe(EmailDeliveryLog.skipped(message, "Mail disabled or no SMTP sender configured"));
            return;
        }

        String effectiveTo = resolveRecipient(message.to());
        if (effectiveTo == null || effectiveTo.isBlank()) {
            log.debug("[EMAIL] Skipped — no effective recipient after mode resolution");
            persistSafe(EmailDeliveryLog.skipped(message, "No effective recipient after mode resolution"));
            return;
        }

        try {
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mime, MimeMessageHelper.MULTIPART_MODE_NO, StandardCharsets.UTF_8.name());
            
            InternetAddress from = (message.from() != null && !message.from().isBlank())
                    ? new InternetAddress(message.from().trim(), message.fromName() != null ? message.fromName().trim() : props.getFromName(), StandardCharsets.UTF_8.name())
                    : fromAddress();
            
            helper.setFrom(from);
            helper.setTo(effectiveTo);
            helper.setSubject(message.subject());
            helper.setText(message.htmlBody(), true);

            if (props.getRecipientMode() == EmailProperties.RecipientMode.CC
                    && hasDefaultRecipient()
                    && !effectiveTo.equalsIgnoreCase(props.getDefaultRecipient().trim())) {
                helper.setCc(props.getDefaultRecipient().trim());
            }

            sender.send(mime);
            log.info("[EMAIL SENT] '{}' -> {}{}", message.subject(), effectiveTo,
                    !effectiveTo.equalsIgnoreCase(message.to()) ? " (redirected from " + message.to() + ")" : "");
            persistSafe(EmailDeliveryLog.sent(message, templateType, communityId));

        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("[EMAIL FAILED] '{}' -> {}: {}", message.subject(), effectiveTo, e.getMessage(), e);
            persistSafe(EmailDeliveryLog.failed(message, templateType, e.getMessage(), communityId));
        }
    }

    @Override
    public void sendAll(List<EmailMessage> messages) {
        if (messages == null || messages.isEmpty()) return;
        messages.parallelStream().forEach(this::send);
    }

    private String resolveRecipient(String originalTo) {
        if (props.getRecipientMode() == EmailProperties.RecipientMode.REDIRECT && hasDefaultRecipient()) {
            return props.getDefaultRecipient().trim();
        }
        return originalTo;
    }

    private boolean hasDefaultRecipient() {
        return props.getDefaultRecipient() != null && !props.getDefaultRecipient().isBlank();
    }

    private InternetAddress fromAddress() throws UnsupportedEncodingException {
        return new InternetAddress(props.getFrom(), props.getFromName(), StandardCharsets.UTF_8.name());
    }

    /**
     * Persists a delivery log entry, swallowing any exception so that a DB issue
     * never causes the send() contract ("never throw") to be violated.
     */
    private void persistSafe(EmailDeliveryLog entry) {
        try {
            deliveryLogRepo.save(entry);
        } catch (Exception ex) {
            log.warn("[EMAIL LOG] Failed to persist delivery log entry: {}", ex.getMessage());
        }
    }

    private Long parseCommunityId(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.parseLong(value); }
        catch (NumberFormatException e) { return null; }
    }
}
