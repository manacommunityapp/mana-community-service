package com.manacommunity.api.mw.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mw_integration_configs")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MwIntegrationConfig {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long organizationId;

    private Boolean freshEnabled;
    private Boolean homesEnabled;
    private Boolean sportsEnabled;

    private BigDecimal baseDeliveryPay;
    private BigDecimal perKmRate;
    private BigDecimal onTimeBonus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (freshEnabled == null) freshEnabled = false;
        if (homesEnabled == null) homesEnabled = false;
        if (sportsEnabled == null) sportsEnabled = false;
        if (baseDeliveryPay == null) baseDeliveryPay = BigDecimal.ZERO;
        if (perKmRate == null) perKmRate = BigDecimal.ZERO;
        if (onTimeBonus == null) onTimeBonus = BigDecimal.ZERO;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
