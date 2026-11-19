package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "generator_service_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneratorServiceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "generator_id", nullable = false)
    private GeneratorAsset generator;

    @Column(name = "service_type", nullable = false, length = 40)
    private String serviceType; // A_CHECK_ROUTINE, B_CHECK_250H, C_CHECK_MAJOR, EMERGENCY_REPAIR, BATTERY_REPLACEMENT

    @Column(name = "service_date", nullable = false)
    private LocalDate serviceDate;

    @Column(name = "run_hours_at_service", nullable = false, precision = 10, scale = 2)
    private BigDecimal runHoursAtService;

    @Column(name = "vendor_name", length = 150)
    private String vendorName;

    @Column(name = "technician_name", length = 120)
    private String technicianName;

    @Column(precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(name = "parts_replaced", columnDefinition = "TEXT")
    private String partsReplaced;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "COMPLETED"; // SCHEDULED, IN_PROGRESS, COMPLETED

    @Column(name = "ticket_id", length = 50)
    private String ticketId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (cost == null) cost = BigDecimal.ZERO;
        if (status == null) status = "COMPLETED";
    }
}
