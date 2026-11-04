package com.manacommunity.api.fresh.catalog.model;

import com.manacommunity.api.vendor.commerce.model.VendorProduct;
import com.manacommunity.api.fresh.vendor.model.FreshVendorCapabilityType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fresh_product_catalog_profile", schema = "manacommunity")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class FreshProductCatalogProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_product_id", nullable = false, unique = true)
    private VendorProduct vendorProduct;

    @Enumerated(EnumType.STRING)
    @Column(name = "capability_type", nullable = false, length = 50)
    private FreshVendorCapabilityType capabilityType;

    @Column(name = "shelf_life_hours")
    private Integer shelfLifeHours;

    @Column(name = "storage_instruction", columnDefinition = "TEXT")
    private String storageInstruction;

    @Column(name = "origin_state", length = 60)
    private String originState;

    @Column(name = "origin_district", length = 60)
    private String originDistrict;

    @Column(name = "is_organic", nullable = false)
    @Builder.Default
    private Boolean isOrganic = false;

    @Column(name = "organic_cert_number", length = 80)
    private String organicCertNumber;

    @Column(name = "is_pesticide_free", nullable = false)
    @Builder.Default
    private Boolean isPesticideFree = false;

    @Column(name = "is_antibiotic_free", nullable = false)
    @Builder.Default
    private Boolean isAntibioticFree = false;

    @Column(name = "is_hormone_free", nullable = false)
    @Builder.Default
    private Boolean isHormoneFree = false;

    @Column(name = "min_order_qty", nullable = false, precision = 10, scale = 3)
    @Builder.Default
    private BigDecimal minOrderQty = BigDecimal.ONE;

    @Column(name = "max_order_qty", precision = 10, scale = 3)
    private BigDecimal maxOrderQty;

    @Column(name = "order_unit", nullable = false, length = 30)
    @Builder.Default
    private String orderUnit = "kg";

    @Enumerated(EnumType.STRING)
    @Column(name = "business_model", nullable = false, length = 40)
    @Builder.Default
    private BusinessModel businessModel = BusinessModel.MARKETPLACE;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_owner_type", nullable = false, length = 40)
    @Builder.Default
    private InventoryOwnerType inventoryOwnerType = InventoryOwnerType.VENDOR;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfilment_owner_type", nullable = false, length = 40)
    @Builder.Default
    private FulfilmentOwnerType fulfilmentOwnerType = FulfilmentOwnerType.VENDOR;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_mode", nullable = false, length = 40)
    @Builder.Default
    private DeliveryMode deliveryMode = DeliveryMode.VENDOR_DELIVERY;

    @Column(name = "seller_of_record", length = 120)
    @Builder.Default
    private String sellerOfRecord = "VENDOR";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() { this.updatedAt = Instant.now(); }
}