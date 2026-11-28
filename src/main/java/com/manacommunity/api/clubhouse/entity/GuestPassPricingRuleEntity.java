package com.manacommunity.api.clubhouse.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "guest_pass_pricing_rules", schema = "clubhouse")
@Getter
@Setter
@NoArgsConstructor
public class GuestPassPricingRuleEntity {

    @Id
    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "facility_code", nullable = false, length = 40)
    private String facilityCode;

    @Column(name = "day_category", nullable = false, length = 20)
    private String dayCategory;

    @Column(name = "unit_price_paise", nullable = false)
    private long unitPricePaise;

    @Column(name = "gst_bps", nullable = false)
    private int gstBps = 0;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}
