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
    private Long towerId;
    private String towerBlock;
    private Long flatId;
    private Long propertyId;
    private Long userId;

    private static final ThreadLocal<TenantContext> CURRENT_CONTEXT = new ThreadLocal<>();

    public static void setContext(TenantContext context) {
        if (context == null) {
            clear();
        } else {
            CURRENT_CONTEXT.set(context);
        }
    }

    public static TenantContext getRawContext() {
        return CURRENT_CONTEXT.get();
    }

    public static boolean isSet() {
        return CURRENT_CONTEXT.get() != null;
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

    public static Long getTowerId() {
        TenantContext ctx = CURRENT_CONTEXT.get();
        return ctx != null ? ctx.towerId : null;
    }

    public static String getTowerBlock() {
        TenantContext ctx = CURRENT_CONTEXT.get();
        return ctx != null ? ctx.towerBlock : null;
    }

    public static Long getFlatId() {
        TenantContext ctx = CURRENT_CONTEXT.get();
        if (ctx == null) return null;
        return ctx.flatId != null ? ctx.flatId : ctx.propertyId;
    }

    public static Long getPropertyId() {
        TenantContext ctx = CURRENT_CONTEXT.get();
        if (ctx == null) return null;
        return ctx.propertyId != null ? ctx.propertyId : ctx.flatId;
    }

    public static Long getUserId() {
        TenantContext ctx = CURRENT_CONTEXT.get();
        return ctx != null ? ctx.userId : null;
    }

    public static Long requireCommunityId() {
        Long cId = getCommunityId();
        if (cId == null) {
            throw new IllegalStateException("Active community tenant context is required but none was resolved");
        }
        return cId;
    }

    public static Long requireOrganizationId() {
        Long orgId = getOrganizationId();
        if (orgId == null) {
            throw new IllegalStateException("Active organization tenant context is required but none was resolved");
        }
        return orgId;
    }

    public static void setTenant(Long organizationId, Long communityId) {
        TenantContext ctx = getContext();
        ctx.setOrganizationId(organizationId != null ? organizationId : DEFAULT_ORG_ID);
        ctx.setCommunityId(communityId);
    }

    public static void setUnitContext(Long towerId, String towerBlock, Long flatId, Long userId) {
        TenantContext ctx = getContext();
        ctx.setTowerId(towerId);
        ctx.setTowerBlock(towerBlock);
        ctx.setFlatId(flatId);
        ctx.setPropertyId(flatId);
        ctx.setUserId(userId);
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

    public TenantContext copy() {
        return TenantContext.builder()
                .organizationId(this.organizationId)
                .communityId(this.communityId)
                .towerId(this.towerId)
                .towerBlock(this.towerBlock)
                .flatId(this.flatId)
                .propertyId(this.propertyId)
                .userId(this.userId)
                .build();
    }

    public static void runWith(TenantContext context, Runnable task) {
        TenantContext previous = CURRENT_CONTEXT.get();
        try {
            setContext(context);
            task.run();
        } finally {
            if (previous != null) {
                setContext(previous);
            } else {
                clear();
            }
        }
    }

    public static <T> T callWith(TenantContext context, java.util.concurrent.Callable<T> task) throws Exception {
        TenantContext previous = CURRENT_CONTEXT.get();
        try {
            setContext(context);
            return task.call();
        } finally {
            if (previous != null) {
                setContext(previous);
            } else {
                clear();
            }
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
