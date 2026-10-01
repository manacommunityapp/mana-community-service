package com.manacommunity.api.finance.personal.repository;

import com.manacommunity.api.finance.personal.entity.PersonalTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalTransactionRepository extends JpaRepository<PersonalTransaction, String>, JpaSpecificationExecutor<PersonalTransaction> {
    List<PersonalTransaction> findByUserIdOrderByTransactionDateDescCreatedAtDesc(Long userId, Pageable pageable);

    Optional<PersonalTransaction> findByIdAndUserId(String id, Long userId);

    List<PersonalTransaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(Long userId, LocalDate from, LocalDate to);

    List<PersonalTransaction> findByUserIdAndIsManaProjectionTrueOrderByTransactionDateDesc(Long userId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PersonalTransaction t WHERE t.user.id = :userId AND t.type = 'INCOME' AND t.transactionDate BETWEEN :from AND :to")
    BigDecimal sumIncomeForPeriod(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PersonalTransaction t WHERE t.user.id = :userId AND t.type = 'EXPENSE' AND t.transactionDate BETWEEN :from AND :to")
    BigDecimal sumExpensesForPeriod(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM PersonalTransaction t WHERE t.user.id = :userId AND t.type = 'EXPENSE' AND t.categoryId = :categoryId AND t.transactionDate BETWEEN :from AND :to")
    BigDecimal sumCategoryExpensesForPeriod(@Param("userId") Long userId, @Param("categoryId") String categoryId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
