package com.manacommunity.api.hrm.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "hr_assets")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class HrAsset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "organization_id", nullable = false) private Long organizationId;
    @Column(name = "asset_name", nullable = false, length = 200) private String assetName;
    @Column(name = "asset_type", nullable = false, length = 50) private String assetType;
    @Column(name = "asset_code", length = 100) private String assetCode;
    @Column(name = "serial_number", length = 200) private String serialNumber;
    @Column(name = "make", length = 100) private String make;
    @Column(name = "model", length = 100) private String model;
    @Column(name = "purchase_date") private LocalDate purchaseDate;
    @Column(name = "purchase_value") private Double purchaseValue;
    @Column(name = "current_value") private Double currentValue;
    @Column(name = "warranty_expiry") private LocalDate warrantyExpiry;
    @Column(name = "status", nullable = false, length = 20) @Builder.Default private String status = "AVAILABLE";
    @Column(name = "notes", columnDefinition = "TEXT") private String notes;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
