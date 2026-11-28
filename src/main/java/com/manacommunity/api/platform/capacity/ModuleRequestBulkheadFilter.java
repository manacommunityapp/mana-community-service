package com.manacommunity.api.platform.capacity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 * Per-instance request bulkheads for high-volume business modules.
 *
 * <p>These non-blocking permits reject excess module traffic before it can occupy
 * the shared Tomcat request pool. Limits are local to this service instance and
 * should be sized so their sum leaves headroom in the configured Tomcat pool.</p>
 */
@Slf4j
@Component
@Order(206)
public class ModuleRequestBulkheadFilter extends OncePerRequestFilter {

    private final Map<Module, Semaphore> permits = new EnumMap<>(Module.class);
    private final Map<Module, Integer> limits = new EnumMap<>(Module.class);

    public ModuleRequestBulkheadFilter(
            @Value("$" + "{mana.capacity.request-bulkhead.sports-max-concurrent:32}") int sportsLimit,
            @Value("$" + "{mana.capacity.request-bulkhead.finance-max-concurrent:48}") int financeLimit,
            @Value("$" + "{mana.capacity.request-bulkhead.visitor-max-concurrent:48}") int visitorLimit,
            @Value("$" + "{mana.capacity.request-bulkhead.other-max-concurrent:48}") int otherLimit,
            @Value("$" + "{server.tomcat.threads.max:200}") int tomcatMaxThreads,
            @Value("$" + "{mana.capacity.request-bulkhead.reserved-tomcat-threads:16}") int reservedThreads) {
        addModule(Module.SPORTS, sportsLimit);
        addModule(Module.FINANCE, financeLimit);
        addModule(Module.VISITOR, visitorLimit);
        addModule(Module.OTHER, otherLimit);

        if (tomcatMaxThreads <= 0 || reservedThreads < 0) {
            throw new IllegalArgumentException("Tomcat thread and reserved-thread limits must be positive");
        }
        int configuredBudget = sportsLimit + financeLimit + visitorLimit + otherLimit;
        if (configuredBudget + reservedThreads > tomcatMaxThreads) {
            log.warn("[ModuleBulkhead] Configured module budget {} plus reserved headroom {} exceeds Tomcat max threads {}. Tune mana.capacity.request-bulkhead.* per instance.",
                    configuredBudget, reservedThreads, tomcatMaxThreads);
        }
        log.info("[ModuleBulkhead] Initialized per-instance limits: sports={}, finance={}, visitor={}, other={}, tomcatMax={}, reserved={}",
                sportsLimit, financeLimit, visitorLimit, otherLimit, tomcatMaxThreads, reservedThreads);
    }

    private void addModule(Module module, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Bulkhead limit for " + module.pathName + " must be positive");
        }
        limits.put(module, limit);
        permits.put(module, new Semaphore(limit, true));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Module module = classify(request);
        Semaphore semaphore = permits.get(module);
        int limit = limits.get(module);

        response.setHeader("X-Module-Bulkhead", module.pathName);
        response.setHeader("X-Module-Concurrency-Limit", Integer.toString(limit));

        if (!semaphore.tryAcquire()) {
            int active = limit - semaphore.availablePermits();
            log.warn("[ModuleBulkhead] Rejected saturated module request: module={}, active={}, limit={}",
                    module.pathName, active, limit);
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setHeader("Retry-After", "1");
            response.setHeader("X-Module-Requests-In-Flight", Integer.toString(active));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"MODULE_CAPACITY_EXCEEDED\",\"module\":\""
                    + module.pathName + "\",\"retryAfterSeconds\":1}");
            return;
        }

        response.setHeader("X-Module-Requests-In-Flight",
                Integer.toString(limit - semaphore.availablePermits()));
        try {
            chain.doFilter(request, response);
        } finally {
            semaphore.release();
        }
    }

    private Module classify(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        if (matches(path, "/api/sports", "/api/tournament", "/api/tournaments",
                "/api/player-categories", "/api/cricheroes", "/api/auction")) {
            return Module.SPORTS;
        }
        if (matches(path, "/api/finance", "/api/wallet", "/api/payments", "/api/payment",
                "/api/admin/payments", "/api/v1/loans", "/api/v1/mf", "/api/v1/fd",
                "/api/v1/recharge", "/api/v1/gold", "/api/v1/split", "/api/v1/bbps",
                "/api/v1/personal-finance", "/personal-finance", "/api/compliance",
                "/api/admin/ledger", "/api/api/finance", "/api/api/v1/finance")) {
            return Module.FINANCE;
        }
        if (matches(path, "/api/visitors", "/api/v1/gate")) {
            return Module.VISITOR;
        }
        return Module.OTHER;
    }

    private boolean matches(String path, String... prefixes) {
        for (String prefix : prefixes) {
            if (path.equals(prefix) || path.startsWith(prefix + "/")) {
                return true;
            }
        }
        return false;
    }

    private enum Module {
        SPORTS("sports"),
        FINANCE("finance"),
        VISITOR("visitor"),
        OTHER("other");

        private final String pathName;

        Module(String pathName) {
            this.pathName = pathName;
        }
    }
}