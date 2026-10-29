package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_name", nullable = false, length = 160)
    private String businessName;

    @Column(name = "contact_person", length = 120)
    private String contactPerson;

    @Column(length = 20)
    private String mobile;

    @Column(length = 120)
    private String email;

    @Column(name = "gst_number", length = 30)
    private String gstNumber;

    @Column(length = 20)
    private String pan;

    @Column(length = 400)
    private String address;

    @Column(length = 80)
    private String city;

    @Column(length = 10)
    private String pincode;

    @Column(length = 500)
    private String categories;

    @Column(name = "service_area", length = 500)
    private String serviceArea;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 20, nullable = false)
    @Builder.Default
    private SupplierVerificationStatus verificationStatus = SupplierVerificationStatus.PENDING;

    @Column(precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    @Column(name = "fulfillment_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal fulfillmentRate = BigDecimal.valueOf(100);

    @Column(name = "on_time_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal onTimeRate = BigDecimal.valueOf(100);

    @Column(name = "total_orders")
    @Builder.Default
    private Integer totalOrders = 0;

    @Column(length = 800)
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(length = 500)
    private String certifications;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "community_id")
    private Long communityId;

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
