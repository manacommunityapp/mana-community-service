package com.manacommunity.api.emergency.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "evacuation_muster_roster")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class EvacuationMusterRoster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "broadcast_id", nullable = false)
    private Long broadcastId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "tower_block", nullable = false, length = 50)
    private String towerBlock;

    @Column(name = "person_category", nullable = false, length = 24)
    private String personCategory; // RESIDENT, DOMESTIC_STAFF, VISITOR

    @Column(name = "person_id")
    private Long personId;

    @Column(name = "person_name", nullable = false, length = 120)
    private String personName;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(name = "flat_number", length = 32)
    private String flatNumber;

    @Builder.Default
    @Column(nullable = false, length = 24)
    private String status = "ALERTED"; // ALERTED, SAFE_AT_ASSEMBLY

    @Column(name = "marked_safe_at")
    private LocalDateTime markedSafeAt;
}
