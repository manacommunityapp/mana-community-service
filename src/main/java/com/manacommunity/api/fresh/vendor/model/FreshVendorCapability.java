package com.manacommunity.api.fresh.vendor.model;

import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.entity.VmsVendor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "fresh_vendor_capability",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_fresh_vendor_capability", columnNames = {"vms_vendor_id", "capability_type"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FreshVendorCapability extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vms_vendor_id", nullable = false)
    private VmsVendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "capability_type", nullable = false, length = 50)
    private FreshVendorCapabilityType capabilityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CapabilityStatus status = CapabilityStatus.PENDING_APPROVAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private AppUser approvedByUser;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "fssai_license_number", length = 100)
    private String fssaiLicenseNumber;

    @Column(name = "fssai_valid_until")
    private LocalDate fssaiValidUntil;

    @Column(name = "facility_address", length = 500)
    private String facilityAddress;

    @Column(name = "cold_storage_available", nullable = false)
    @Builder.Default
    private Boolean coldStorageAvailable = false;

    @Column(name = "daily_capacity_units", nullable = false)
    @Builder.Default
    private Integer dailyCapacityUnits = 0;

    @Column(name = "operating_days", nullable = false, length = 50)
    @Builder.Default
    private String operatingDays = "MON,TUE,WED,THU,FRI,SAT,SUN";
}
