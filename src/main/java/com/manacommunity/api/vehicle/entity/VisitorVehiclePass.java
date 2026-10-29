package com.manacommunity.api.vehicle.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitor_vehicle_pass")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitorVehiclePass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "resident_user_id", nullable = false)
    private Long residentUserId;

    @Column(name = "plate_number", nullable = false, length = 30)
    private String plateNumber;

    @Column(name = "visitor_name", length = 100)
    private String visitorName;

    @Column(name = "valid_date", nullable = false)
    private LocalDate validDate;

    @Column(length = 20, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
