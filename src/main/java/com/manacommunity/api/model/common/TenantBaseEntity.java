package com.manacommunity.api.model.common;

import com.manacommunity.api.config.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class TenantBaseEntity extends BaseAuditEntity {

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @PrePersist
    public void populateTenantContext() {
        if (this.organizationId == null) {
            this.organizationId = TenantContext.getOrganizationId();
        }
        if (this.communityId == null) {
            this.communityId = TenantContext.getCommunityId();
        }
    }
}
