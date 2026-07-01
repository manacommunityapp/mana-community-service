package com.manacommunity.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "asset_audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "asset_id", nullable = false)
    private InventoryItem asset;

    @Column(name = "audited_at", nullable = false)
    private LocalDateTime auditedAt;

    @Column(name = "audited_by", length = 120)
    private String auditedBy;

    @Column(name = "expected_status", length = 30)
    private String expectedStatus;

    @Column(name = "actual_status", length = 30)
    private String actualStatus;

    @Column(name = "expected_quantity")
    private Integer expectedQuantity;

    @Column(name = "actual_quantity")
    private Integer actualQuantity;

    @Column(name = "variance")
    private Integer variance;

    @Column(length = 255)
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (auditedAt == null) {
            auditedAt = LocalDateTime.now();
        }
    }
}
