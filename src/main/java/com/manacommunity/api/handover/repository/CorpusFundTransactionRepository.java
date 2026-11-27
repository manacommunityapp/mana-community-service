package com.manacommunity.api.handover.repository;

import com.manacommunity.api.handover.entity.CorpusFundTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorpusFundTransactionRepository extends JpaRepository<CorpusFundTransaction, Long> {

    List<CorpusFundTransaction> findByFinancialYear(String fy);

    @Query("SELECT t FROM CorpusFundTransaction t WHERE t.status = 'DISPUTED' ORDER BY t.disputeRaisedAt DESC")
    List<CorpusFundTransaction> findDisputedTransactions();

    @Query("SELECT t FROM CorpusFundTransaction t WHERE t.frozen = true ORDER BY t.transactionDate DESC")
    List<CorpusFundTransaction> findFrozenTransactions();

    @Query("SELECT t FROM CorpusFundTransaction t WHERE t.type = 'ESCROW_HOLD' AND t.status != 'WRITTEN_OFF'")
    List<CorpusFundTransaction> findActiveEscrowHolds();

    @Query("SELECT t.type, SUM(t.netAmount) FROM CorpusFundTransaction t GROUP BY t.type")
    List<Object[]> sumByType();

    @Query("SELECT t.financialYear, t.monthNumber, SUM(t.netAmount) FROM CorpusFundTransaction t GROUP BY t.financialYear, t.monthNumber ORDER BY t.financialYear, t.monthNumber")
    List<Object[]> monthlyTrend();

    @Query("SELECT t.category, SUM(t.netAmount) FROM CorpusFundTransaction t WHERE t.type NOT IN ('BUILDER_DEPOSIT', 'OPENING_BALANCE', 'INTEREST_EARNED') GROUP BY t.category")
    List<Object[]> expenseByCategory();
}
