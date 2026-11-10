package com.manacommunity.api.sports.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sports_official", indexes = {
    @Index(name = "idx_official_sport_role", columnList = "sport, official_role, is_available")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsOfficial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 50)
    private String sport;

    @Column(name = "official_role", nullable = false, length = 50)
    private String officialRole;

    @Column(name = "badge_level", nullable = false, length = 50)
    private String badgeLevel;

    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal dailyRate = new BigDecimal("1500.00");

    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal hourlyRate = new BigDecimal("300.00");

    @Column(name = "matches_officiated", nullable = false)
    @Builder.Default
    private Integer matchesOfficiated = 0;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = new BigDecimal("5.00");

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
