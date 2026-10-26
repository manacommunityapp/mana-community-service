package com.manacommunity.api.model;

import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_care_booking", indexes = {
    @Index(name = "idx_vc_booking_resident", columnList = "resident_id"),
    @Index(name = "idx_vc_booking_community", columnList = "community_id"),
    @Index(name = "idx_vc_booking_slot", columnList = "slot_id"),
    @Index(name = "idx_vc_booking_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCareBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_number", nullable = false, unique = true, length = 30)
    private String bookingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resident_id", nullable = false)
    private AppUser resident;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleProfile vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private VehicleServiceCatalog service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private VehicleServiceSlot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private AppUser technician;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING_PAYMENT";

    @Column(name = "base_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "addon_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal addonAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_order_id", length = 100)
    private String paymentOrderId;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "location_notes", length = 300)
    private String locationNotes;

    @Column(name = "technician_notes", columnDefinition = "TEXT")
    private String technicianNotes;

    @Column(name = "rating")
    private Integer rating;

    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;

    @Column(name = "eta_minutes")
    private Integer etaMinutes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
