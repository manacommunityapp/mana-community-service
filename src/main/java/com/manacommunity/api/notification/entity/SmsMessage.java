package com.manacommunity.api.notification.entity;

import com.manacommunity.api.notification.enums.MessageType;
import com.manacommunity.api.notification.enums.SmsLanguage;
import com.manacommunity.api.notification.enums.SmsPriority;
import com.manacommunity.api.notification.enums.SmsStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sms_message", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nullable — anonymous/system sends don't have a user */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(name = "template_code", length = 100)
    private String templateCode;

    @Column(name = "rendered_body", columnDefinition = "TEXT", nullable = false)
    private String renderedBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private MessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SmsStatus status = SmsStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private SmsPriority priority = SmsPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private SmsLanguage language = SmsLanguage.EN;

    /** Provider-assigned SID (e.g. Twilio MessageSid) */
    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;

    @Column(name = "provider_name", length = 30)
    private String providerName;

    @Column(name = "provider_response", columnDefinition = "TEXT")
    private String providerResponse;

    @Column(name = "retry_count")
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private int maxRetries = 3;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    /** Segments consumed (160 chars = 1 segment; longer splits into multiple) */
    @Column(name = "segments")
    @Builder.Default
    private int segments = 1;

    @Column(name = "cost_usd", precision = 10)
    private java.math.BigDecimal costUsd;

    /** Bulk campaign FK — null for standalone messages */
    @Column(name = "campaign_id")
    private Long campaignId;

    /** Correlation to the originating entity (e.g. eventId, bookingId) */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
