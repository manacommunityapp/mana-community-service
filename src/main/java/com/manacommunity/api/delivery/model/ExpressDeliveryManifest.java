package com.manacommunity.api.delivery.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "express_delivery_manifests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpressDeliveryManifest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "partner", nullable = false, length = 32)
    private DeliveryPartner partner;

    @Column(name = "manifest_code", nullable = false, unique = true, length = 32)
    private String manifestCode;

    @Column(name = "rider_name", nullable = false, length = 120)
    private String riderName;

    @Column(name = "rider_phone", nullable = false, length = 32)
    private String riderPhone;

    @Column(name = "vehicle_number", length = 32)
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private ExpressPassStatus status = ExpressPassStatus.PRE_APPROVED;

    @Column(name = "total_packages", nullable = false)
    @Builder.Default
    private Integer totalPackages = 0;

    @Column(name = "distinct_flats", nullable = false)
    @Builder.Default
    private Integer distinctFlats = 0;

    @Column(name = "delivered_packages", nullable = false)
    @Builder.Default
    private Integer deliveredPackages = 0;

    @Column(name = "pending_packages", nullable = false)
    @Builder.Default
    private Integer pendingPackages = 0;

    @Column(name = "entry_gate", length = 64)
    private String entryGate;

    @Column(name = "exit_gate", length = 64)
    private String exitGate;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
