package com.manacommunity.api.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_blacklist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "plate_number", nullable = false, length = 30)
    private String plateNumber;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "blacklisted_by_user_id")
    private Long blacklistedByUserId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
