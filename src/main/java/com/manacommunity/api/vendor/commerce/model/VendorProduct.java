package com.manacommunity.api.vendor.commerce.model;

import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.common.BaseAuditEntity;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.entity.VmsVendor;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vms_vendor_id")
    private VmsVendor vendor;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "product_code", length = 60)
    private String productCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 30)
    @Builder.Default
    private ProductType productType = ProductType.PHYSICAL_PRODUCT;

    /** Legacy free-text category kept for backward compatibility with existing data. */
    @Column(nullable = false, length = 80)
    private String category;

    @Column(name = "sub_category", length = 80)
    private String subCategory;

    /** Structured category linked to commerce_product_category table. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commerce_category_id")
    private CommerceProductCategory commerceCategory;

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

    /** Comma-separated additional image URLs. */
    @Column(name = "additional_images", columnDefinition = "TEXT")
    private String additionalImages;

    /** Whether the product is visible only to community members (not public). */
    @Column(name = "community_only", nullable = false)
    @Builder.Default
    private Boolean communityOnly = false;

    @Column(name = "weight_grams")
    private Integer weightGrams;

    /** e.g. "30x20x10" in cm */
    @Column(name = "dimensions_cm", length = 60)
    private String dimensionsCm;

    /** Comma-separated search/filter tags */
    @Column(columnDefinition = "TEXT")
    private String tags;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VendorProductVariant> variants = new ArrayList<>();

    public enum ProductType {
        PHYSICAL_PRODUCT,
        DIGITAL_PRODUCT,
        SERVICE,
        FOOD,
        BOOKING,
        SUBSCRIPTION
    }
}
