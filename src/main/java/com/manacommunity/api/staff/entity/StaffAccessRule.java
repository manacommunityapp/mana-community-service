package com.manacommunity.api.staff.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_access_rule")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffAccessRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "staff_id", nullable = false)
    private Long staffId;

    @Column(name = "day_of_week", length = 10, nullable = false)
    private String dayOfWeek;

    @Column(name = "allowed_from", nullable = false)
    private LocalTime allowedFrom;

    @Column(name = "allowed_until", nullable = false)
    private LocalTime allowedUntil;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
