package com.manacommunity.api.vendor.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vms_vendor_social_media")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VmsVendorSocialMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private VmsVendor vendor;

    @Column(nullable = false, length = 50)
    private String platform;

    @Column(nullable = false, length = 500)
    private String url;
}
