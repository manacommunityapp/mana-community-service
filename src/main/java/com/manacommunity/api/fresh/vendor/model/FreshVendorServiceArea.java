package com.manacommunity.api.fresh.vendor.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.vendor.entity.VmsVendor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(
    name = "fresh_vendor_service_area",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_fresh_vendor_community", columnNames = {"vms_vendor_id", "community_id"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class FreshVendorServiceArea extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vms_vendor_id", nullable = false)
    private VmsVendor vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @Column(name = "cutoff_time", nullable = false)
    @Builder.Default
    private LocalTime cutoffTime = LocalTime.of(22, 0);

    @Column(name = "morning_delivery_slot", nullable = false, length = 50)
    @Builder.Default
    private String morningDeliverySlot = "06:00_08:00";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
