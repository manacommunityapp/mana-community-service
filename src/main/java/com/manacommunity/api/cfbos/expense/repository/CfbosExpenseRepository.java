package com.manacommunity.api.cfbos.expense.repository;
import com.manacommunity.api.cfbos.expense.entity.CfbosExpense;
import com.manacommunity.api.cfbos.expense.enums.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CfbosExpenseRepository extends JpaRepository<CfbosExpense, Long> {
    Optional<CfbosExpense> findByExpenseNumber(String expenseNumber);
    List<CfbosExpense> findByCommunityId(Long communityId);
    List<CfbosExpense> findByCommunityIdAndStatus(Long communityId, ExpenseStatus status);
    List<CfbosExpense> findByStatus(ExpenseStatus status);
    List<CfbosExpense> findByExpenseCategoryId(Long categoryId);
}
