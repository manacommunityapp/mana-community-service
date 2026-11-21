package com.manacommunity.api.finance.repository;

import com.manacommunity.api.config.tenant.TenantContext;
import com.manacommunity.api.finance.entity.FinanceInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FinanceInvoiceRepository extends JpaRepository<FinanceInvoice, Long> {

    // ── Community-scoped queries (use these) ────────────────────────────────────

    List<FinanceInvoice> findByCommunityIdOrderByDocDateDesc(Long communityId);

    List<FinanceInvoice> findByCommunityIdAndStatusOrderByDocDateDesc(Long communityId, String status);

    Optional<FinanceInvoice> findByIdAndCommunityId(Long id, Long communityId);

    boolean existsByIdAndCommunityId(Long id, Long communityId);

    @Query("SELECT COALESCE(SUM(i.grandTotal), 0) FROM FinanceInvoice i " +
            "WHERE i.status <> 'Paid' AND i.communityId = :communityId")
    BigDecimal sumOutstandingReceivables(@Param("communityId") Long communityId);

    // ── Legacy, platform-wide signatures ───────────────────────────────────────

    /**
     * Kept only so existing callers still compile; it now resolves the community from
     * {@link TenantContext} (populated from the authenticated principal by
     * {@code TenantFilter}) instead of aggregating every tenant's receivables.
     * With no tenant in context it fails closed and returns 0 rather than leaking a
     * platform-wide total.
     */
    default BigDecimal sumOutstandingReceivables() {
        return sumOutstandingReceivables(TenantContext.getCommunityId());
    }

    /** @deprecated unscoped — leaks every community's invoices. Use the communityId variants. */
    @Deprecated
    List<FinanceInvoice> findAllByOrderByDocDateDesc();

    /** @deprecated unscoped — leaks every community's invoices. Use the communityId variants. */
    @Deprecated
    List<FinanceInvoice> findByStatusOrderByDocDateDesc(String status);
}
