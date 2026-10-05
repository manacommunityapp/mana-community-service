package com.manacommunity.api.vendor.commerce.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vendor_product")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorProduct extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id", nullable = false)
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_user_id", nullable = false)
    private AppUser vendorUser;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(name = "sub_category", length = 80)
    private String subCategory;

    @Column(length = 120)
    private String brand;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "hsn_code", length = 30)
    private String hsnCode;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstRate = BigDecimal.valueOf(5.00);

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "image_url")
    private String imageUrl;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VendorProductVariant> variants = new ArrayList<>();
}
