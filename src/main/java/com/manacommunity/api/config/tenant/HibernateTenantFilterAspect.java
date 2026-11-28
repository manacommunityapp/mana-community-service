package com.manacommunity.api.config.tenant;

import com.manacommunity.api.model.common.TenantBaseEntity;
import com.manacommunity.api.platform.tenant.TenantContextHolder;
import com.manacommunity.api.platform.tenant.TenantQueryTimeoutService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Automatically enables Hibernate's {@code tenantCommunityFilter}, binds PostgreSQL's
 * {@code app.current_community_id} session setting (RLS), and enforces per-tenant
 * {@code statement_timeout} during {@code @Transactional} boundaries whenever either
 * {@link TenantContext#getCommunityId()} or {@link TenantContextHolder#get()} is present.
 */
@Slf4j
@Aspect
@Component
public class HibernateTenantFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private TenantQueryTimeoutService queryTimeoutService;

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setQueryTimeoutService(TenantQueryTimeoutService queryTimeoutService) {
        this.queryTimeoutService = queryTimeoutService;
    }

    @Around("@annotation(org.springframework.transaction.annotation.Transactional) || @within(org.springframework.transaction.annotation.Transactional)")
    public Object applyTenantFilterAroundTransaction(ProceedingJoinPoint pjp) throws Throwable {
        Long activeCommunityId = resolveActiveCommunityId();
        boolean filterEnabled = false;
        if (activeCommunityId != null) {
            filterEnabled = enableCommunityFilter(entityManager, activeCommunityId);
            applyPostgresRlsContext(activeCommunityId);
            applyPostgresStatementTimeout(activeCommunityId);
        }
        try {
            return pjp.proceed();
        } finally {
            if (filterEnabled) {
                disableCommunityFilter(entityManager);
            }
        }
    }

    public Long resolveActiveCommunityId() {
        Long fromTenantContext = TenantContext.getCommunityId();
        if (fromTenantContext != null) {
            return fromTenantContext;
        }
        return TenantContextHolder.get();
    }

    public boolean enableCommunityFilter(EntityManager em, Long communityId) {
        if (em == null || communityId == null || !em.isOpen()) {
            return false;
        }
        try {
            Session session = em.unwrap(Session.class);
            if (session != null) {
                session.enableFilter(TenantBaseEntity.TENANT_COMMUNITY_FILTER)
                        .setParameter("communityId", communityId);
                return true;
            }
        } catch (Exception e) {
            log.debug("Could not enable Hibernate tenant filter for community {}: {}", communityId, e.getMessage());
        }
        return false;
    }

    public void disableCommunityFilter(EntityManager em) {
        if (em == null || !em.isOpen()) {
            return;
        }
        try {
            Session session = em.unwrap(Session.class);
            if (session != null && session.getEnabledFilter(TenantBaseEntity.TENANT_COMMUNITY_FILTER) != null) {
                session.disableFilter(TenantBaseEntity.TENANT_COMMUNITY_FILTER);
            }
        } catch (Exception ignored) {}
    }

    public void applyPostgresRlsContext(Long communityId) {
        if (jdbcTemplate == null || communityId == null) {
            return;
        }
        try {
            jdbcTemplate.update("SELECT set_config('app.current_community_id', ?, true)", String.valueOf(communityId));
        } catch (Exception e) {
            log.debug("Non-PostgreSQL or non-transactional datasource skipped set_config: {}", e.getMessage());
        }
    }

    /**
     * Bounds PostgreSQL query execution time within the current transaction so one
     * tenant's heavy report/AGM vote query cannot pin DB connections indefinitely.
     */
    public void applyPostgresStatementTimeout(Long communityId) {
        if (jdbcTemplate == null || communityId == null) {
            return;
        }
        long timeoutMs = queryTimeoutService != null ? queryTimeoutService.getStatementTimeoutMs(communityId) : 5000L;
        try {
            jdbcTemplate.update("SELECT set_config('statement_timeout', ?, true)", String.valueOf(timeoutMs));
        } catch (Exception e) {
            log.debug("Non-PostgreSQL or non-transactional datasource skipped statement_timeout: {}", e.getMessage());
        }
    }
}
