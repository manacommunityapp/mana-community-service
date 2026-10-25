package com.manacommunity.api.market.bazaar.entity;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_booking", indexes = @Index(name = "idx_market_booking_status_expiry", columnList = "status, hold_expires_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MarketBooking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private MarketApplication application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stall_id", nullable = false)
    private MarketStall stall;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private AppUser applicant;

    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 5) @Builder.Default private String currency = "INR";
    @Column(nullable = false, length = 24) @Builder.Default private String status = "PAYMENT_PENDING";
    @Column(name = "hold_expires_at", nullable = false) private LocalDateTime holdExpiresAt;
    @Column(name = "payment_order_id", length = 50) private String paymentOrderId;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }
    @PreUpdate protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
