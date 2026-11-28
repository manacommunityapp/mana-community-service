package com.manacommunity.api.dailyhelp.entity;

import com.manacommunity.api.model.Community;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "society_staff_affiliation", schema = "manacommunity",
        uniqueConstraints = @UniqueConstraint(name = "uq_society_staff", columnNames = {"community_id", "staff_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocietyStaffAffiliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private GlobalStaffProfile staffProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "local_badge_id", nullable = false, length = 32)
    private String localBadgeId;

    @Builder.Default
    @Column(name = "current_gate_state", nullable = false, length = 10)
    private String currentGateState = "OUT";

    @Column(name = "last_gate_event_at")
    private LocalDateTime lastGateEventAt;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
