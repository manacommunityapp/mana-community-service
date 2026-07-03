package com.manacommunity.api.finance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A finance-module customer (billed party for invoices, estimates, sales orders,
 * credit notes and receipts). Richer than the retail customer: carries GST
 * details, currency, opening balance and billing / shipping addresses to match
 * the "New Customer" drawer in the Income &amp; Expense UI.
 */
@Entity
@Table(name = "ledger_customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 160)
    private String email;

    @Column(length = 40)
    private String phone;

    @Column(name = "gst_type", length = 40)
    private String gstType;

    @Column(length = 20)
    private String gstin;

    @Column(length = 8)
    private String currency;

    @Column(name = "opening_balance", precision = 14, scale = 2)
    private BigDecimal openingBalance;

    /** "Cr" or "Dr". */
    @Column(name = "balance_type", length = 4)
    private String balanceType;

    @Column(name = "starts_from")
    private LocalDate startsFrom;

    // ── Billing address ──
    @Column(name = "bill_addr1", length = 200) private String billAddr1;
    @Column(name = "bill_addr2", length = 200) private String billAddr2;
    @Column(name = "bill_city", length = 80)   private String billCity;
    @Column(name = "bill_state", length = 80)  private String billState;
    @Column(name = "bill_country", length = 80) private String billCountry;
    @Column(name = "bill_zipcode", length = 20) private String billZipcode;

    // ── Shipping address ──
    @Column(name = "ship_addr1", length = 200) private String shipAddr1;
    @Column(name = "ship_addr2", length = 200) private String shipAddr2;
    @Column(name = "ship_city", length = 80)   private String shipCity;
    @Column(name = "ship_state", length = 80)  private String shipState;
    @Column(name = "ship_country", length = 80) private String shipCountry;
    @Column(name = "ship_zipcode", length = 20) private String shipZipcode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
