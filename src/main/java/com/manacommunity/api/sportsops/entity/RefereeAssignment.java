package com.manacommunity.api.sportsops.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "referee_assignment")
@Data
public class RefereeAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "referee_id", nullable = false) private Long refereeId;
    @Column(name = "event_id", nullable = false) private Long eventId;
    @Column(name = "event_type", nullable = false, length = 100) private String eventType;
    @Column(nullable = false, length = 100) private String sport;
    @Column(name = "assigned_at") private Instant assignedAt = Instant.now();
    @Column private Boolean confirmed = false;
    @Column(name = "confirmed_at") private Instant confirmedAt;
    @Column(precision = 12, scale = 2) private BigDecimal fee;
    @Column(length = 500) private String notes;
}
