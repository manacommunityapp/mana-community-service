package com.manacommunity.api.sportsops.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;

@Entity
@Table(name = "coach_package_enrollment")
@Data
public class CoachPackageEnrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "community_id", nullable = false) private Long communityId;
    @Column(name = "package_id", nullable = false) private Long packageId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "sessions_used") private Integer sessionsUsed = 0;
    @Column(name = "enrolled_at") private Instant enrolledAt = Instant.now();
    @Column(name = "expires_at") private Instant expiresAt;
    @Column private Boolean active = true;
}
