package com.manacommunity.api.repository;

import com.manacommunity.api.model.BudgetAllocation;
import com.manacommunity.api.model.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetAllocationRepository extends JpaRepository<BudgetAllocation, Long> {
    Optional<BudgetAllocation> findByFinancialYearAndCategory(String financialYear, ExpenseCategory category);
    List<BudgetAllocation> findByFinancialYear(String financialYear);
    List<BudgetAllocation> findByFinancialYearOrderByCategoryAsc(String financialYear);
}
