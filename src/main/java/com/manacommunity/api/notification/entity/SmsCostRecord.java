package com.manacommunity.api.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One row per SMS sent — used for cost tracking and monthly billing reports. */
@Entity
@Table(name = "sms_cost_record", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsCostRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sms_message_id", nullable = false)
    private Long smsMessageId;

    @Column(name = "provider_name", nullable = false, length = 30)
    private String providerName;

    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;

    @Column(name = "to_number", nullable = false, length = 20)
    private String toNumber;

    @Column(name = "segments", nullable = false)
    @Builder.Default
    private int segments = 1;

    @Column(name = "cost_usd", nullable = false, precision = 12, scale = 6)
    @Builder.Default
    private BigDecimal costUsd = BigDecimal.ZERO;

    @Column(name = "cost_inr", precision = 12, scale = 4)
    private BigDecimal costInr;

    @Column(name = "billing_date", nullable = false)
    private LocalDate billingDate;

    @Column(name = "message_type", length = 30)
    private String messageType;

    @Column(name = "community_id")
    private Long communityId;

    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false)
    private LocalDateTime recordedAt;
}
