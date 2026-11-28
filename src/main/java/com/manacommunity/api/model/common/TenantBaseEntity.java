package com.manacommunity.api.model.common;

import com.manacommunity.api.config.tenant.TenantContext;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@MappedSuperclass
@FilterDef(
        name = TenantBaseEntity.TENANT_COMMUNITY_FILTER,
        parameters = @ParamDef(name = "communityId", type = Long.class)
)
@Filter(
        name = TenantBaseEntity.TENANT_COMMUNITY_FILTER,
        condition = "community_id = :communityId"
)
@Getter
@Setter
public abstract class TenantBaseEntity extends BaseAuditEntity {

    public static final String TENANT_COMMUNITY_FILTER = "tenantCommunityFilter";

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @PrePersist
    public void populateTenantContext() {
        Long currentCommunityId = TenantContext.getCommunityId();

        if (this.communityId == null) {
            if (currentCommunityId == null) {
                throw new IllegalStateException(
                        "Tenant identity (communityId) is mandatory for tenant-owned write on "
                                + getClass().getSimpleName());
            }
            this.communityId = currentCommunityId;
        } else {
            // Cross-tenant write prevention: if communityId is explicitly specified,
            // it MUST match the active tenant context to prevent IDOR injection
            if (currentCommunityId != null && !this.communityId.equals(currentCommunityId)) {
                throw new IllegalStateException(
                        "Cross-tenant write rejected: entity community (" + this.communityId
                                + ") does not match active tenant context (" + currentCommunityId + ")");
            }
        }

        Long currentOrgId = TenantContext.getOrganizationId();
        if (this.organizationId == null) {
            this.organizationId = (currentOrgId != null) ? currentOrgId : TenantContext.DEFAULT_ORG_ID;
        } else if (currentOrgId != null && !this.organizationId.equals(currentOrgId)) {
            throw new IllegalStateException(
                    "Cross-tenant write rejected: entity organization (" + this.organizationId
                            + ") does not match active tenant context (" + currentOrgId + ")");
        }
    }

    @PreUpdate
    public void validateTenantContextOnUpdate() {
        Long currentCommunityId = TenantContext.getCommunityId();
        if (currentCommunityId != null && this.communityId != null && !this.communityId.equals(currentCommunityId)) {
            throw new IllegalStateException(
                    "Cross-tenant update rejected: cannot mutate entity community (" + this.communityId
                            + ") across tenant boundary (" + currentCommunityId + ")");
        }
    }
}
