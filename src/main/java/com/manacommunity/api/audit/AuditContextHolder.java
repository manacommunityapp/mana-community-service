package com.manacommunity.api.audit;

public class AuditContextHolder {

    private static final ThreadLocal<AuditContext> CONTEXT_HOLDER = new ThreadLocal<>();

    public static void setContext(AuditContext context) {
        CONTEXT_HOLDER.set(context);
    }

    public static AuditContext getContext() {
        AuditContext ctx = CONTEXT_HOLDER.get();
        if (ctx == null) {
            ctx = new AuditContext();
            CONTEXT_HOLDER.set(ctx);
        }
        return ctx;
    }

    public static void clearContext() {
        CONTEXT_HOLDER.remove();
    }
}
