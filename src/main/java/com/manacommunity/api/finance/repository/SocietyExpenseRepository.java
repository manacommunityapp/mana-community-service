package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.SocietyExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface SocietyExpenseRepository extends JpaRepository<SocietyExpense, Long> {

    List<SocietyExpense> findByCommunityIdOrderByCreatedAtDesc(Long communityId);

    List<SocietyExpense> findByCommunityIdAndStatusOrderByCreatedAtDesc(Long communityId, SocietyExpense.ApprovalStatus status);

    long countByCommunityIdAndStatusIn(Long communityId, List<SocietyExpense.ApprovalStatus> statuses);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM SocietyExpense e WHERE e.communityId = :communityId AND e.status = :status")
    BigDecimal sumAmountByCommunityIdAndStatus(@Param("communityId") Long communityId, @Param("status") SocietyExpense.ApprovalStatus status);
}
