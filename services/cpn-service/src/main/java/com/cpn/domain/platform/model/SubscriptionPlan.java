package com.cpn.domain.platform.model;

import com.cpn.domain.common.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "subscription_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan extends TenantAwareEntity {

    @Column(nullable = false)
    private String planCode; // FREE, PRO, ENTERPRISE

    @Column(nullable = false)
    private String planName;

    private BigDecimal monthlyPrice;
    private BigDecimal annualPrice;
    private Integer maxMembers;
    private boolean isCustomDomainAllowed;
    private boolean isAiAssistantUnlimited;
}
