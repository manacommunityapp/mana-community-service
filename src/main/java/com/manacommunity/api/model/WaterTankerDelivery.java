package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "water_tanker_deliveries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterTankerDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "delivery_date", nullable = false)
    @Builder.Default
    private LocalDateTime deliveryDate = LocalDateTime.now();

    @Column(name = "vendor_name", nullable = false, length = 150)
    private String vendorName;

    @Column(name = "vehicle_number", nullable = false, length = 50)
    private String vehicleNumber;

    @Column(name = "capacity_liters", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal capacityLiters = BigDecimal.valueOf(12000);

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal cost = BigDecimal.valueOf(1200);

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(name = "driver_phone", length = 50)
    private String driverPhone;

    @Column(name = "target_tank_id")
    private Long targetTankId;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "RECEIVED"; // RECEIVED, PENDING_VERIFICATION, REJECTED

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
