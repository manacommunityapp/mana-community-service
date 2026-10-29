package com.manacommunity.api.mobility.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "mobility_vehicle")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class MobilityVehicle {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private MobilityDriver driver;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private MobilityVehicleType vehicleType;

    @Column(length = 60)
    private String make;

    @Column(nullable = false, length = 60)
    private String model;

    @Column(name = "number_plate", nullable = false, length = 20)
    private String numberPlate;

    @Column(length = 40)
    private String color;

    private Integer year;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
