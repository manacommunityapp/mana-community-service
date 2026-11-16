package com.manacommunity.api.fresh.processing.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fresh_batch_release", schema = "manacommunity")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FreshBatchRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false, unique = true)
    private FreshProcessingBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "released_by_user_id", nullable = false)
    private AppUser releasedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "release_decision", nullable = false, length = 30)
    private ReleaseDecision releaseDecision;

    @Column(name = "temperature_verified_celsius", nullable = false, precision = 4, scale = 1)
    private BigDecimal temperatureVerifiedCelsius;

    @Column(name = "released_at", nullable = false)
    @Builder.Default
    private Instant releasedAt = Instant.now();

    @Column(length = 500)
    private String remarks;
}