package com.cpn.domain.auth.model;

import com.cpn.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String tenantCode;

    @Column(nullable = false)
    private String tenantName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantType tenantType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan subscriptionPlan;
    
    private boolean isActive = true;

    public enum TenantType {
        CORPORATE, EDUCATIONAL, COMMUNITY
    }

    public enum SubscriptionPlan {
        FREE, PRO, ENTERPRISE
    }
}
