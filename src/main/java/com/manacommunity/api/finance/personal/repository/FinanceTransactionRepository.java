package com.manacommunity.api.finance.personal.repository;

import com.manacommunity.api.finance.personal.entity.FinanceTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface FinanceTransactionRepository extends JpaRepository<FinanceTransaction, Long> {

    Page<FinanceTransaction> findByUserIdOrderByTxnDateDescCreatedAtDesc(Long userId, Pageable pageable);

    Page<FinanceTransaction> findByUserIdAndAccountIdOrderByTxnDateDescCreatedAtDesc(Long userId, Long accountId, Pageable pageable);

    List<FinanceTransaction> findByUserIdAndTxnDateBetweenOrderByTxnDateDesc(Long userId, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinanceTransaction t WHERE t.user.id = :userId AND t.txnType = :txnType AND t.txnDate BETWEEN :from AND :to")
    BigDecimal sumByUserAndTypeAndDateRange(Long userId, FinanceTransaction.TxnType txnType, LocalDate from, LocalDate to);

    @Query("SELECT t.category.id, t.category.name, COALESCE(SUM(t.amount), 0) " +
           "FROM FinanceTransaction t " +
           "WHERE t.user.id = :userId AND t.txnType = 'EXPENSE' AND t.txnDate BETWEEN :from AND :to AND t.category IS NOT NULL " +
           "GROUP BY t.category.id, t.category.name ORDER BY SUM(t.amount) DESC")
    List<Object[]> sumExpensesByCategory(Long userId, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM FinanceTransaction t " +
           "WHERE t.user.id = :userId AND t.txnType = 'EXPENSE' AND t.category.id = :categoryId AND t.txnDate BETWEEN :from AND :to")
    BigDecimal sumExpensesByCategoryAndDateRange(Long userId, Long categoryId, LocalDate from, LocalDate to);
}
