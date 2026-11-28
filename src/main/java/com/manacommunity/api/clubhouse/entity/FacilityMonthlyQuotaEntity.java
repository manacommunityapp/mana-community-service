package com.manacommunity.api.clubhouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "facility_monthly_quotas", schema = "clubhouse")
@Getter
@Setter
@NoArgsConstructor
public class FacilityMonthlyQuotaEntity {

    @EmbeddedId
    private FacilityMonthlyQuotaId id;

    @Column(name = "free_limit", nullable = false)
    private short freeLimit = 2;

    @Column(name = "free_consumed", nullable = false)
    private short freeConsumed = 0;

    @Column(name = "free_reserved", nullable = false)
    private short freeReserved = 0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public int freeRemaining() {
        return Math.max(0, freeLimit - (freeConsumed + freeReserved));
    }
}
