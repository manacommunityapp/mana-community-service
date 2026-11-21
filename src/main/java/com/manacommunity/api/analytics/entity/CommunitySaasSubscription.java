package com.manacommunity.api.analytics.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "community_saas_subscription")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommunitySaasSubscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "external_reference", unique = true, length = 120)
    private String externalReference;

    @Column(name = "plan_code", nullable = false, length = 60)
    private String planCode;

    @Column(name = "plan_name", nullable = false, length = 120)
    private String planName;

    @Column(name = "recurring_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal recurringAmount;

    @Column(name = "billing_period_months", nullable = false)
    private int billingPeriodMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "current_period_start", nullable = false)
    private LocalDate currentPeriodStart;

    @Column(name = "current_period_end")
    private LocalDate currentPeriodEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum Status { ACTIVE, TRIALING, PAST_DUE, PAUSED, CANCELLED, EXPIRED }
}
