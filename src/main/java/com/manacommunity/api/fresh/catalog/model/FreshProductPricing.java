package com.manacommunity.api.fresh.catalog.model;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.vendor.entity.VmsVendor;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "fresh_product_pricing", schema = "manacommunity")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FreshProductPricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_product_id", nullable = false)
    private VendorProduct vendorProduct;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "vendor_cost_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal vendorCostPrice;

    @Column(name = "mana_commission_pct", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal manaCommissionPct = BigDecimal.valueOf(10.00);

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal deliveryFee = BigDecimal.ZERO;

    @Column(name = "packaging_fee", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal packagingFee = BigDecimal.ZERO;

    @Column(name = "gst_rate_pct", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstRatePct = BigDecimal.valueOf(5.00);

    @Column(name = "resident_selling_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal residentSellingPrice;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_vendor_id")
    private VmsVendor createdByVendor;

    @Column(name = "admin_approved", nullable = false)
    @Builder.Default
    private Boolean adminApproved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_approved_by")
    private AppUser adminApprovedBy;

    @Column(name = "admin_approved_at")
    private Instant adminApprovedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }

    /** Utility: compute resident selling price from cost components */
    public static BigDecimal computeSellingPrice(
            BigDecimal costPrice,
            BigDecimal commissionPct,
            BigDecimal deliveryFee,
            BigDecimal packagingFee,
            BigDecimal gstPct) {
        BigDecimal base = costPrice
                .add(costPrice.multiply(commissionPct).divide(BigDecimal.valueOf(100)))
                .add(deliveryFee)
                .add(packagingFee);
        BigDecimal gst = base.multiply(gstPct).divide(BigDecimal.valueOf(100));
        return base.add(gst).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}