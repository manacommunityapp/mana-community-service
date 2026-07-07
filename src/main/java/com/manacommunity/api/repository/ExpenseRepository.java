package com.manacommunity.api.repository;

import com.manacommunity.api.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    // ── Listing (community-scoped) ──────────────────────────────────────────
    Page<Expense> findByCommunityIdOrderByCreatedAtDesc(Long communityId, Pageable pageable);

    Page<Expense> findByCommunityIdAndStatusOrderByCreatedAtDesc(
            Long communityId, String status, Pageable pageable);

    // ── Safe single-row fetch (prevents cross-community access) ──────────────
    Optional<Expense> findByIdAndCommunityId(Long id, Long communityId);

    // ── Dashboard summary aggregates ────────────────────────────────────────
    long countByCommunityId(Long communityId);

    long countByCommunityIdAndStatus(Long communityId, String status);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.community.id = :communityId")
    BigDecimal sumAmountByCommunity(@Param("communityId") Long communityId);

    @Query("select coalesce(sum(e.amount), 0) from Expense e "
            + "where e.community.id = :communityId and e.status = :status")
    BigDecimal sumAmountByCommunityAndStatus(@Param("communityId") Long communityId,
                                             @Param("status") String status);
}
