package com.manacommunity.api.vehicle.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicle_registration")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRegistration extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private AppUser owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "plate_number", nullable = false, length = 20)
    private String plateNumber;

    @Column(name = "vehicle_type", length = 30)
    @Builder.Default
    private String vehicleType = "CAR";

    @Column(length = 80)
    private String make;

    @Column(length = 80)
    private String model;

    @Column(length = 40)
    private String color;

    @Column(name = "is_gate_whitelisted")
    @Builder.Default
    private Boolean isGateWhitelisted = false;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";
}
