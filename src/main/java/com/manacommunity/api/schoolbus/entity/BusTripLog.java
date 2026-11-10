package com.manacommunity.api.schoolbus.entity;

import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "school_bus_trip_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusTripLog extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @Column(name = "pickup_time")
    private LocalDateTime pickupTime;

    @Column(name = "drop_time")
    private LocalDateTime dropTime;

    @Column(length = 20)
    @Builder.Default
    private String attendance = "PRESENT";

    @Column(name = "bus_id")
    private Long busId;

    @Column(name = "driver_name", length = 100)
    private String driverName;
}
