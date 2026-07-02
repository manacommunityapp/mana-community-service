package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(length = 80)
    private String location;

    @Column(name = "serial_number", length = 80)
    private String serialNumber;

    @Column(name = "vendor_name", length = 120)
    private String vendorName;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "warranty_expiry_date")
    private LocalDate warrantyExpiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ItemStatus status;

    @Column(name = "original_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalCost;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal tco;

    @Column(name = "qr_code_id", unique = true, length = 40)
    private String qrCodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "depreciation_method", length = 30)
    private DepreciationMethod depreciationMethod;

    @Column(name = "useful_life_months")
    private Integer usefulLifeMonths;

    @Column(name = "salvage_value", precision = 12, scale = 2)
    private BigDecimal salvageValue;

    @Column(name = "current_value", precision = 12, scale = 2)
    private BigDecimal currentValue;

    @Column(name = "next_maintenance_due_at")
    private LocalDate nextMaintenanceDueAt;

    @Column(name = "last_audited_at")
    private LocalDateTime lastAuditedAt;

    @Column(name = "audit_variance")
    private Integer auditVariance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private String borrowedBy;

    @Transient
    private String borrowedByFlat;

    @Transient
    private String borrowedAt;

    @Transient
    private String expectedReturn;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ItemStatus.AVAILABLE;
        }
        if (originalCost == null) {
            originalCost = BigDecimal.ZERO;
        }
        if (tco == null) {
            tco = originalCost;
        }
        if (currentValue == null) {
            currentValue = originalCost;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
