package com.manacommunity.api.config.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantContext implements AutoCloseable {

    public static final Long DEFAULT_ORG_ID = 1L;
    public static final String DEFAULT_SCHEMA = "manacommunity";

    private Long organizationId;
    private Long communityId;
    private Long propertyId;
    private Long userId;

    private static final InheritableThreadLocal<TenantContext> CURRENT_CONTEXT = new InheritableThreadLocal<>();

    public static void setContext(TenantContext context) {
        CURRENT_CONTEXT.set(context);
    }

    public static TenantContext getContext() {
        TenantContext ctx = CURRENT_CONTEXT.get();
        if (ctx == null) {
            ctx = TenantContext.builder()
                    .organizationId(DEFAULT_ORG_ID)
                    .build();
            CURRENT_CONTEXT.set(ctx);
        }
        return ctx;
    }

    public static Long getOrganizationId() {
        TenantContext ctx = CURRENT_CONTEXT.get();
        return (ctx != null && ctx.organizationId != null) ? ctx.organizationId : DEFAULT_ORG_ID;
    }

    public static Long getCommunityId() {
        TenantContext ctx = CURRENT_CONTEXT.get();
        return ctx != null ? ctx.communityId : null;
    }

    public static void setTenant(Long organizationId, Long communityId) {
        TenantContext ctx = getContext();
        ctx.setOrganizationId(organizationId != null ? organizationId : DEFAULT_ORG_ID);
        ctx.setCommunityId(communityId);
    }

    public static String getTenantId() {
        Long cId = getCommunityId();
        return cId != null ? "community_" + cId : DEFAULT_SCHEMA;
    }

    public static void setTenantId(String tenantId) {
        if (tenantId != null && tenantId.startsWith("community_")) {
            try {
                Long id = Long.parseLong(tenantId.substring(10));
                getContext().setCommunityId(id);
            } catch (NumberFormatException ignored) {}
        }
    }

    public static void clear() {
        CURRENT_CONTEXT.remove();
    }

    @Override
    public void close() {
        clear();
    }
}
