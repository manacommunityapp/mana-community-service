package com.manacommunity.api.parking.entity;

import com.manacommunity.api.model.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@Entity
@Table(name = "parking_bookings")
@Data
@EqualsAndHashCode(callSuper = true)
public class ParkingBooking extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "bay_id", nullable = false)
    private Long bayId;

    @Column(name = "vehicle_number", nullable = false)
    private String vehicleNumber;

    @Column(name = "vehicle_type", nullable = false)
    private String vehicleType; // SEDAN, SUV, BIKE, EV

    @Column(name = "visitor_name")
    private String visitorName;

    @Column(name = "host_flat", nullable = false)
    private String hostFlat;

    @Column(name = "booking_type", nullable = false)
    private String bookingType; // VISITOR, TEMPORARY, EV_CHARGE

    @Column(nullable = false)
    private String status; // ACTIVE, COMPLETED, WRONG_PARK, VIOLATION

    @Column(name = "check_in_time", nullable = false)
    private Instant checkInTime;

    @Column(name = "check_out_time")
    private Instant checkOutTime;

    @Column(name = "assigned_basement")
    private String assignedBasement;

    @Column(name = "assigned_bay_number")
    private String assignedBayNumber;

    @Column(name = "actual_bay_id")
    private Long actualBayId; // if parked in wrong bay

    @Column(name = "wrong_park_detected")
    private Boolean wrongParkDetected = false;

    @Column(name = "violation_type")
    private String violationType; // WRONG_SLOT, FIRE_LANE, OVERSTAY, DOUBLE_PARK
}
