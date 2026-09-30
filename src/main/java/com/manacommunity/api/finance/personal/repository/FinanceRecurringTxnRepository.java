package com.manacommunity.api.finance.personal.repository;

import com.manacommunity.api.finance.personal.entity.FinanceRecurringTxn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FinanceRecurringTxnRepository extends JpaRepository<FinanceRecurringTxn, Long> {

    List<FinanceRecurringTxn> findByUserIdOrderByNextDueDateAsc(Long userId);

    List<FinanceRecurringTxn> findByUserIdAndActiveTrueOrderByNextDueDateAsc(Long userId);

    List<FinanceRecurringTxn> findByActiveTrueAndNextDueDateLessThanEqual(LocalDate date);
}
