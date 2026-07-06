package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.FinanceBusinessExpense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceBusinessExpenseRepository extends JpaRepository<FinanceBusinessExpense, Long> {
    List<FinanceBusinessExpense> findAllByOrderByDocDateDesc();
    List<FinanceBusinessExpense> findByStatusOrderByDocDateDesc(String status);
}
