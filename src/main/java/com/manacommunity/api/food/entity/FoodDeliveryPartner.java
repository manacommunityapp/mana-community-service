package com.manacommunity.api.food.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_delivery_partners")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodDeliveryPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Column(name = "vehicle_number", length = 20)
    private String vehicleNumber;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PartnerStatus status = PartnerStatus.OFFLINE;

    @Column(name = "current_latitude", precision = 10, scale = 7)
    private BigDecimal currentLatitude;

    @Column(name = "current_longitude", precision = 10, scale = 7)
    private BigDecimal currentLongitude;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "total_deliveries")
    @Builder.Default
    private Integer totalDeliveries = 0;

    @Column(name = "active_zone", length = 100)
    private String activeZone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

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
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum VehicleType { BICYCLE, MOTORCYCLE, CAR }

    public enum PartnerStatus { AVAILABLE, ON_DELIVERY, OFFLINE, SUSPENDED }
}
