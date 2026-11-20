package com.manacommunity.api.model;

import com.manacommunity.api.resident.model.CommunityFlat;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_profile", indexes = {
    @Index(name = "idx_vehicle_profile_owner", columnList = "owner_id"),
    @Index(name = "idx_vehicle_profile_community", columnList = "community_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false, length = 80)
    private String nickname;

    @Column(name = "vehicle_type", nullable = false, length = 20)
    private String vehicleType; // CAR, BIKE, SUV, TRUCK

    @Column(nullable = false, length = 50)
    private String make;

    @Column(nullable = false, length = 80)
    private String model;

    @Column(length = 30)
    private String color;

    @Column(name = "plate_number", nullable = false, length = 20)
    private String plateNumber;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flat_id")
    private CommunityFlat flat;

    @Column(name = "rfid_tag", length = 100)
    private String rfidTag;

    @Column(name = "parking_slot", length = 50)
    private String parkingSlot;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
