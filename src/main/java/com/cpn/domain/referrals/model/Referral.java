package com.cpn.domain.referrals.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "referrals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Referral extends TenantAwareEntity {

    @Column(nullable = false)
    private UUID referrerId;

    private UUID refereeId;

    @Column(nullable = false)
    private String jobTitle;

    private String companyName;
    private BigDecimal bountyAmount;

    @Column(nullable = false)
    private String status; // OPEN, SUBMITTED, HIRED, CLOSED
}
