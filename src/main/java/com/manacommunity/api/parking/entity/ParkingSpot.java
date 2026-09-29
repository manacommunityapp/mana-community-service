package com.manacommunity.api.parking.entity;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parking_spot", schema = "manacommunity")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class ParkingSpot extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "spot_number", nullable = false, length = 50)
    private String spotNumber;

    @Column(name = "level", nullable = false, length = 50)
    private String level;

    @Column(name = "spot_type", nullable = false, length = 20)
    @Builder.Default
    private String spotType = "CAR"; // CAR, BIKE, EV

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "AVAILABLE"; // AVAILABLE, OCCUPIED, RESERVED

    @Column(name = "vehicle_number", length = 50)
    private String vehicleNumber;

    @Column(name = "owner_name", length = 100)
    private String ownerName;

    @Column(name = "owner_flat", length = 50)
    private String ownerFlat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_user_id")
    private AppUser assignedUser;

    @Column(name = "notes", length = 255)
    private String notes;
}
