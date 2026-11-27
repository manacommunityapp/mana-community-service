package com.manacommunity.api.parking.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "parking_bays", uniqueConstraints = @UniqueConstraint(columnNames = {"community_id", "basement", "bay_number"}))
@Data
@EqualsAndHashCode(callSuper = true)
public class ParkingBay extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(nullable = false)
    private String basement; // B1, B2, B3

    @Column(name = "bay_number", nullable = false)
    private String bayNumber;

    @Column(name = "bay_type", nullable = false)
    private String bayType; // VISITOR, RESIDENT, EV_CHARGING, RESERVED

    @Column(name = "vehicle_clearance", nullable = false)
    private String vehicleClearance; // SEDAN, SUV, ANY

    @Column(name = "max_height_cm")
    private Integer maxHeightCm;

    @Column(name = "floor_distance_rank")
    private Integer floorDistanceRank; // proximity to entry/exit, lower = closer

    @Column(nullable = false)
    private String status; // AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE

    @Column(name = "assigned_flat")
    private String assignedFlat; // for RESIDENT bays

    @Column(name = "current_vehicle_number")
    private String currentVehicleNumber;

    @Column(name = "current_booking_id")
    private Long currentBookingId;

    @Column(name = "has_ev_charger")
    private Boolean hasEvCharger = false;

    @Column(name = "ev_charger_id")
    private Long evChargerId;

    @Version
    private Long version;
}
