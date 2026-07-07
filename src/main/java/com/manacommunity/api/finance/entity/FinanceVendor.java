package com.manacommunity.api.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A finance-module vendor (the supplier side of the Income &amp; Expense UI —
 * the party paid for purchases, purchase orders and debit notes). Mirrors the
 * "New Vendor" form: contact details, GSTIN / PAN and a billing address.
 *
 * <p>Named {@code FinanceVendor} to avoid clashing with the retail
 * {@code com.manacommunity.api.model.Vendor} entity / repository bean.
 */
@Entity(name = "FinanceVendor")
@Table(name = "finance_vendors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceVendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "contact_person", length = 160)
    private String contactPerson;

    @Column(length = 160)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(length = 20)
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

    /** "Active" or "Inactive". */
    @Column(length = 20)
    @Builder.Default
    private String status = "Active";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
