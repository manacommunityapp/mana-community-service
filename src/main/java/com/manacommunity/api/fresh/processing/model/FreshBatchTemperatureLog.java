package com.manacommunity.api.fresh.processing.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fresh_batch_temperature_log", schema = "manacommunity")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FreshBatchTemperatureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private FreshProcessingBatch batch;

    @Column(name = "logged_temp_celsius", nullable = false, precision = 4, scale = 1)
    private BigDecimal loggedTempCelsius;

    @Column(name = "location_tag", nullable = false, length = 100)
    private String locationTag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logged_by_user_id")
    private AppUser loggedByUser;

    @Column(name = "logged_at", nullable = false)
    @Builder.Default
    private Instant loggedAt = Instant.now();
}