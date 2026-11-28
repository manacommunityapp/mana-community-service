package com.manacommunity.api.emergency.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "evacuation_broadcasts")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EvacuationBroadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "tower_block", nullable = false, length = 50)
    private String towerBlock;

    @Column(name = "emergency_type", nullable = false, length = 32)
    private String emergencyType;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String severity = "CRITICAL";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "assembly_point", length = 200)
    private String assemblyPoint;

    @Builder.Default
    @Column(name = "residents_count", nullable = false)
    private int residentsCount = 0;

    @Builder.Default
    @Column(name = "staff_count", nullable = false)
    private int staffCount = 0;

    @Builder.Default
    @Column(name = "visitors_count", nullable = false)
    private int visitorsCount = 0;

    @Builder.Default
    @Column(name = "sms_fallback_count", nullable = false)
    private int smsFallbackCount = 0;

    @Column(name = "triggered_by")
    private Long triggeredBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
