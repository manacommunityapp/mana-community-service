package com.manacommunity.api.healthguard.entity;

import com.manacommunity.api.healthguard.enums.HealthGuardEnums.WearableEventType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wearable_health_event", schema = "manacommunity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WearableHealthEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Long deviceId;

    @Column(nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private WearableEventType eventType;

    private Integer heartRateBpm;

    @Column(precision = 5, scale = 2)
    private BigDecimal spo2Percent;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(columnDefinition = "text")
    private String rawData;

    @Builder.Default
    private Boolean sosTriggered = false;

    private Long linkedIncidentId;

    @Column(nullable = false)
    private LocalDateTime eventAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (eventAt == null) eventAt = LocalDateTime.now();
        createdAt = LocalDateTime.now();
    }
}
