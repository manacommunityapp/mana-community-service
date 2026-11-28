package com.manacommunity.api.homeservices.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
    name = "morning_sub_overrides",
    uniqueConstraints = @UniqueConstraint(name = "uq_morning_override_plan_date", columnNames = {"plan_id", "delivery_date"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MorningSubOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "delivery_date", nullable = false)
    private LocalDate deliveryDate;

    @Builder.Default
    @Column(name = "override_qty", nullable = false)
    private Integer overrideQty = 1;

    @Builder.Default
    @Column(nullable = false)
    private Boolean paused = false;

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
