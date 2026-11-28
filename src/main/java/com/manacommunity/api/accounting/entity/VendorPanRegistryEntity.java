package com.manacommunity.api.accounting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "vendor_pan_registry", schema = "accounting")
@Getter
@Setter
@NoArgsConstructor
public class VendorPanRegistryEntity {

    @Id
    @Column(name = "pan", nullable = false, length = 10)
    private String pan;

    @Column(name = "entity_type_char", nullable = false, length = 1)
    private String entityTypeChar;

    @Column(name = "pan_status", nullable = false, length = 30)
    private String panStatus = "VALID";

    @Column(name = "sec_206ab_specified", nullable = false)
    private boolean sec206abSpecified = false;

    @Column(name = "lower_deduction_rate", precision = 5, scale = 2)
    private BigDecimal lowerDeductionRate;

    @Column(name = "lower_deduction_cap")
    private Long lowerDeductionCap;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt = Instant.now();
}
