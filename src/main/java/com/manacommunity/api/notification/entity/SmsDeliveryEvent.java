package com.manacommunity.api.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** Stores each webhook delivery status update from the SMS provider for idempotency. */
@Entity
@Table(name = "sms_delivery_event", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sms_delivery_provider_event",
                columnNames = {"provider_message_id", "event_type"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsDeliveryEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sms_message_id")
    private Long smsMessageId;

    /** SID / MsgId returned by the SMS provider */
    @Column(name = "provider_message_id", nullable = false, length = 100)
    private String providerMessageId;

    /** e.g. "delivered", "failed", "queued" */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /** Raw webhook payload stored for debugging / audit */
    @Column(name = "raw_payload", columnDefinition = "TEXT")
    private String rawPayload;

    /** Provider-side timestamp of the delivery event */
    @Column(name = "event_timestamp")
    private LocalDateTime eventTimestamp;

    @CreationTimestamp
    @Column(name = "received_at", updatable = false)
    private LocalDateTime receivedAt;
}
