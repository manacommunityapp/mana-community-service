package com.manacommunity.api.fresh.processing.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "fresh_batch_quality_check", schema = "manacommunity")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FreshBatchQualityCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private FreshProcessingBatch batch;

    @Column(name = "check_name", nullable = false, length = 100)
    private String checkName;

    @Column(name = "measured_value", nullable = false, length = 100)
    private String measuredValue;

    @Column(name = "is_passed", nullable = false)
    private Boolean isPassed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checked_by_user_id", nullable = false)
    private AppUser checkedByUser;

    @Column(name = "checked_at", nullable = false)
    @Builder.Default
    private Instant checkedAt = Instant.now();

    @Column(length = 500)
    private String notes;
}