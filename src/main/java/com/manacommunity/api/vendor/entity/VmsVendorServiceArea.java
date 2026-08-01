package com.manacommunity.api.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vms_vendor_service_areas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsVendorServiceArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VmsVendor vendor;

    @Column(name = "area_name", nullable = false, length = 150)
    private String areaName;

    @Column(length = 100)
    private String city;

    @Column(length = 10)
    private String pincode;

    @Column(name = "radius_km", precision = 6, scale = 2)
    private BigDecimal radiusKm;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
