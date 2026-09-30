package com.manacommunity.api.finance.personal.repository;

import com.manacommunity.api.finance.personal.entity.FinanceAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface FinanceAccountRepository extends JpaRepository<FinanceAccount, Long> {

    List<FinanceAccount> findByUserIdAndActiveTrueOrderByAccountNameAsc(Long userId);

    List<FinanceAccount> findByUserIdOrderByAccountNameAsc(Long userId);

    @Query("SELECT COALESCE(SUM(a.balance), 0) FROM FinanceAccount a WHERE a.user.id = :userId AND a.active = true AND a.includeInTotal = true")
    BigDecimal sumBalanceByUserId(Long userId);

    int countByUserIdAndActiveTrue(Long userId);
}
