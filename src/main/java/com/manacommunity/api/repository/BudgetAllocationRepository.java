package com.manacommunity.api.repository;

import com.manacommunity.api.model.BudgetAllocation;
import com.manacommunity.api.model.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetAllocationRepository extends JpaRepository<BudgetAllocation, Long> {
    Optional<BudgetAllocation> findByCommunityIdAndFinancialYearAndCategory(Long communityId, String financialYear, ExpenseCategory category);
    List<BudgetAllocation> findByCommunityIdOrderByCategoryAsc(Long communityId);
    List<BudgetAllocation> findByCommunityIdAndFinancialYearOrderByCategoryAsc(Long communityId, String financialYear);
    void deleteByCommunityIdAndId(Long communityId, Long id);

    /** @deprecated Use community-scoped variants; retained for backward compatibility with existing data. */
    @Deprecated
    Optional<BudgetAllocation> findByFinancialYearAndCategory(String financialYear, ExpenseCategory category);
    @Deprecated
    List<BudgetAllocation> findByFinancialYear(String financialYear);
    @Deprecated
    List<BudgetAllocation> findByFinancialYearOrderByCategoryAsc(String financialYear);
}
