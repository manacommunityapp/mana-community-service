package com.manacommunity.api.vendor.commerce.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.manacommunity.api.model.common.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    @Column(name = "unit_of_measure", length = 30)
    private String unitOfMeasure;

    // ── Core pricing (legacy precision 12,2 — kept for backward compat) ─────────
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal mrp;

    @Column(name = "vendor_cost", precision = 12, scale = 2, nullable = false)
    private BigDecimal vendorCost;

    @Column(name = "default_community_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal defaultCommunityPrice;

    // ── Community Pricing Tiers (precision 19,4 per master-prompt standard) ──────
    /** Price for verified MEMBER role — optional, falls back to defaultCommunityPrice. */
    @Column(name = "member_price", precision = 19, scale = 4)
    private BigDecimal memberPrice;

    /** Minimum quantity for bulk price to apply. */
    @Column(name = "bulk_min_qty")
    @Builder.Default
    private Integer bulkMinQty = 5;

    /** Price per unit when ordered in bulk (≥ bulkMinQty). */
    @Column(name = "bulk_price", precision = 19, scale = 4)
    private BigDecimal bulkPrice;

    /** Limited-time promotional price. */
    @Column(name = "promotional_price", precision = 19, scale = 4)
    private BigDecimal promotionalPrice;

    /** Date until promotional price is valid (inclusive). */
    @Column(name = "promotional_end_date")
    private LocalDate promotionalEndDate;

    /** Festival or occasion tag (e.g. "DIWALI", "EID", "CHRISTMAS"). */
    @Column(name = "festival_tag", length = 60)
    private String festivalTag;

    /** Special festival pricing. */
    @Column(name = "festival_price", precision = 19, scale = 4)
    private BigDecimal festivalPrice;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToOne(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    private VendorProductInventory inventory;

    /**
     * Resolves the best price for the given buyer role and quantity.
     * Priority: promotionalPrice > festivalPrice > memberPrice > bulkPrice > defaultCommunityPrice.
     */
    public BigDecimal resolvePrice(String buyerRole, int quantity) {
        if (promotionalPrice != null && promotionalEndDate != null &&
            !LocalDate.now().isAfter(promotionalEndDate)) {
            return promotionalPrice;
        }
        if (festivalPrice != null && festivalTag != null && !festivalTag.isBlank()) {
            return festivalPrice;
        }
        if (memberPrice != null && "MEMBER".equals(buyerRole)) {
            return memberPrice;
        }
        if (bulkPrice != null && bulkMinQty != null && quantity >= bulkMinQty) {
            return bulkPrice;
        }
        return defaultCommunityPrice;
    }
}
