package com.manacommunity.api.email;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Persistent record of every email dispatch attempt.
 *
 * <p>Written by {@link SmtpEmailService} after every {@code send()} call — whether
 * the dispatch succeeded, failed, or was skipped (mail disabled / no recipient).
 * The {@code trackingToken} field is reserved for Level-2 open-tracking: a UUID
 * injected into a 1×1 pixel URL inside the HTML body; when the recipient opens
 * the email, {@code openedAt} is populated via a public tracking endpoint.</p>
 */
@Entity
@Table(name = "email_delivery_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailDeliveryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Actual recipient address (after redirect/CC mode resolution). */
    @Column(nullable = false, length = 255)
    private String recipient;

    /** Email subject line. */
    @Column(length = 500)
    private String subject;

    /**
     * Logical template type for filtering in the admin dashboard.
     * Examples: OTP, MATCH_REMINDER, TOURNAMENT_START, PRIZE_DISTRIBUTION.
     */
    @Column(name = "template_type", length = 100)
    private String templateType;

    /** Outcome: SENT, FAILED, or SKIPPED. */
    @Column(nullable = false, length = 20)
    private String status;

    /** SMTP or internal error message — populated when status = FAILED. */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * UUID token stamped into the open-tracking pixel URL.
     * Null until open-tracking is enabled (Level 2 implementation).
     */
    @Column(name = "tracking_token", length = 64, unique = true)
    private String trackingToken;

    /** Sender address / display name (e.g., "Mana Community <support@manacommunity.com>"). */
    @Column(name = "sender", length = 255)
    private String sender;

    /** Full rendered HTML or text body of the email for preview & resending. */
    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    /** Timestamp when the recipient opened the email (pixel loaded). Null until then. */
    @Column(name = "opened_at")
    private LocalDateTime openedAt;

    /** Community the email was sent from — links the log entry to a tenant. */
    @Column(name = "community_id")
    private Long communityId;

    /** When the send was attempted. */
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (sentAt == null) sentAt = now;
    }

    // ── Factory helpers ────────────────────────────────────────────────────

    public static final String STATUS_SENT    = "SENT";
    public static final String STATUS_FAILED  = "FAILED";
    public static final String STATUS_SKIPPED = "SKIPPED";

    public static EmailDeliveryLog sent(EmailMessage msg, String templateType, Long communityId) {
        return EmailDeliveryLog.builder()
                .sender(msg.from() != null ? msg.from() : null)
                .recipient(msg.to())
                .subject(msg.subject())
                .body(msg.htmlBody())
                .templateType(templateType)
                .status(STATUS_SENT)
                .communityId(communityId)
                .sentAt(LocalDateTime.now())
                .build();
    }

    public static EmailDeliveryLog failed(EmailMessage msg, String templateType,
                                          String errorMessage, Long communityId) {
        return EmailDeliveryLog.builder()
                .sender(msg.from() != null ? msg.from() : null)
                .recipient(msg.to())
                .subject(msg.subject())
                .body(msg.htmlBody())
                .templateType(templateType)
                .status(STATUS_FAILED)
                .errorMessage(errorMessage)
                .communityId(communityId)
                .sentAt(LocalDateTime.now())
                .build();
    }

    public static EmailDeliveryLog skipped(EmailMessage msg, String reason) {
        return EmailDeliveryLog.builder()
                .sender(msg.from() != null ? msg.from() : null)
                .recipient(msg.to() != null ? msg.to() : "unknown")
                .subject(msg.subject())
                .body(msg.htmlBody())
                .status(STATUS_SKIPPED)
                .errorMessage(reason)
                .sentAt(LocalDateTime.now())
                .build();
    }
}
