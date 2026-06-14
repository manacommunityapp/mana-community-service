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

        try {
            MimeMessage mime = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    mime, MimeMessageHelper.MULTIPART_MODE_NO, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress());
            helper.setTo(message.to());
            helper.setSubject(message.subject());
            helper.setText(message.htmlBody(), true); // true => HTML
            sender.send(mime);
            log.info("[EMAIL SENT] '{}' -> {}", message.subject(), message.to());
        } catch (MessagingException | UnsupportedEncodingException e) {
            // Never propagate — a failed notification must not break the business flow.
            log.error("[EMAIL FAILED] '{}' -> {}: {}", message.subject(), message.to(), e.getMessage(), e);
        }
    }

    @Override
    public void sendAll(List<EmailMessage> messages) {
        if (messages == null || messages.isEmpty()) return;
        for (EmailMessage m : messages) {
            send(m);
        }
    }

    private InternetAddress fromAddress() throws UnsupportedEncodingException {
        return new InternetAddress(props.getFrom(), props.getFromName(), StandardCharsets.UTF_8.name());
    }
}
