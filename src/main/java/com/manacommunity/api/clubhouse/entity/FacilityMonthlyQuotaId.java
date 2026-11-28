package com.manacommunity.api.clubhouse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacilityMonthlyQuotaId implements Serializable {

    @Column(name = "society_id", nullable = false)
    private Long societyId;

    @Column(name = "facility_code", nullable = false, length = 40)
    private String facilityCode;

    @Column(name = "flat_id", nullable = false)
    private Long flatId;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;
}
