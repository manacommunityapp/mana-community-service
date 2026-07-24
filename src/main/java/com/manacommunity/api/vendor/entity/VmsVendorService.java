package com.manacommunity.api.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vms_vendor_services")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsVendorService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VmsVendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private VmsVendorCategory category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal basePrice = BigDecimal.ZERO;

    @Column(name = "price_unit", length = 30)
    @Builder.Default
    private String priceUnit = "FIXED";

    @Column(name = "discount_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "membership_price", precision = 12, scale = 2)
    private BigDecimal membershipPrice;

    @Column(name = "emergency_price", precision = 12, scale = 2)
    private BigDecimal emergencyPrice;

    @Column(name = "gst_percent", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstPercent = BigDecimal.ZERO;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "warranty_days")
    @Builder.Default
    private Integer warrantyDays = 0;

    @Column(name = "cancellation_policy", length = 1000)
    private String cancellationPolicy;

    @Column(name = "required_equipment", length = 500)
    private String requiredEquipment;

    @Column(name = "required_staff")
    @Builder.Default
    private Integer requiredStaff = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.ACTIVE;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

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

    public enum ServiceStatus { ACTIVE, INACTIVE, DELETED }
}
