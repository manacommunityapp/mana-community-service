package com.manacommunity.api.sports.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sports_coach", indexes = {
    @Index(name = "idx_sports_coach_vendor", columnList = "vendor_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SportsCoach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id")
    private Long communityId;

    @Column(name = "vendor_id")
    private Long vendorId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 30)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "experience_years", nullable = false)
    @Builder.Default
    private Integer experienceYears = 1;

    @Column(columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String sports = "[]";

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String certifications = "[]";

    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal hourlyRate = new BigDecimal("500.00");

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = new BigDecimal("5.00");

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
