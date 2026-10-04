package com.manacommunity.api.vendor.commerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "vendor_product_variant")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorProductVariant extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private VendorProduct product;

    @Column(name = "variant_name", nullable = false, length = 120)
    private String variantName;

    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Column(length = 80)
    private String barcode;

    @Column(name = "pack_size", nullable = false, length = 50)
    private String packSize;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal mrp;

    @Column(name = "vendor_cost", precision = 12, scale = 2, nullable = false)
    private BigDecimal vendorCost;

    @Column(name = "default_community_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal defaultCommunityPrice;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToOne(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private VendorProductInventory inventory;
}
