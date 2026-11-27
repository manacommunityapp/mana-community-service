package com.manacommunity.api.accounting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "vendor_fy_tds_ledger", schema = "accounting")
@Getter
@Setter
@NoArgsConstructor
public class VendorFyTdsLedgerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "fy_code", nullable = false, length = 10)
    private String fyCode;

    @Column(name = "vendor_pan", nullable = false, length = 10)
    private String vendorPan;

    @Column(name = "tds_section", nullable = false, length = 20)
    private String tdsSection;

    @Column(name = "cumulative_gross_paise", nullable = false)
    private long cumulativeGrossPaise = 0L;

    @Column(name = "cumulative_tds_paise", nullable = false)
    private long cumulativeTdsPaise = 0L;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
