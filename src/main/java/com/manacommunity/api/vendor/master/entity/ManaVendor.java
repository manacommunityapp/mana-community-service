package com.manacommunity.api.vendor.master.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "mana_vendor",
    indexes = {
        @Index(name = "idx_mana_vendor_community", columnList = "community_id"),
        @Index(name = "idx_mana_vendor_name",      columnList = "community_id,name")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManaVendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nullable for legacy-migrated rows; required for all new vendors created via API. */
    @Column(name = "community_id")
    private Long communityId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "contact_person", length = 160)
    private String contactPerson;

    @Column(length = 160)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(length = 30)
    private String gstin;

    @Column(length = 20)
    private String pan;

    @Column(length = 300)
    private String address;

    @Column(length = 80)
    private String city;

    @Column(length = 80)
    private String state;

    @Column(length = 20)
    private String pincode;

    @Column(name = "bank_details", length = 255)
    private String bankDetails;

    @Column(length = 200)
    private String categories;

    @Column
    private Integer rating;

    /** 'FINANCE' | 'PROCUREMENT' | 'MANUAL' */
    @Column(name = "source_module", nullable = false, length = 30)
    @Builder.Default
    private String sourceModule = "FINANCE";

    @Column(name = "legacy_vendor_id")
    private Long legacyVendorId;

    @Column(name = "legacy_finance_vendor_id")
    private Long legacyFinanceVendorId;

    /** 'ACTIVE' | 'INACTIVE' */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
