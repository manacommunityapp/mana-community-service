package com.manacommunity.api.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
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

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final EmailProperties props;

    public SmtpEmailService(ObjectProvider<JavaMailSender> mailSenderProvider, EmailProperties props) {
        this.mailSenderProvider = mailSenderProvider;
        this.props = props;
    }

    @Override
    public void send(EmailMessage message) {
        if (message == null || message.to() == null || message.to().isBlank()) {
            log.debug("[EMAIL] Skipped — no recipient address");
            return;
        }

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (!props.isEnabled() || sender == null) {
            log.info("[EMAIL DISABLED] Would send '{}' to {} ({} chars of HTML)",
                    message.subject(), message.to(),
                    message.htmlBody() == null ? 0 : message.htmlBody().length());
            return;
        }

        String effectiveTo = resolveRecipient(message.to());
        if (effectiveTo == null || effectiveTo.isBlank()) {
            log.debug("[EMAIL] Skipped — no effective recipient after mode resolution");
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
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("[EMAIL FAILED] '{}' -> {}: {}", message.subject(), effectiveTo, e.getMessage(), e);
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
}
