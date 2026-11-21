package com.manacommunity.api.sportsops.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "coach_package")
@Data
public class CoachPackage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "coach_id", nullable = false) private Long coachId;
    @Column(nullable = false, length = 100) private String sport;
    @Column(nullable = false, length = 200) private String name;
    @Column(length = 1000) private String description;
    @Column(nullable = false) private Integer sessions;
    @Column(name = "duration_minutes", nullable = false) private Integer durationMinutes;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(name = "group_size", nullable = false) private Integer groupSize = 1;
    @Column private Boolean active = true;
    @Column(name = "created_at") private Instant createdAt = Instant.now();
}
