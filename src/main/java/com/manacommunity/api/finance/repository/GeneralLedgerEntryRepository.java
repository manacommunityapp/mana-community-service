package com.manacommunity.api.finance.repository;

import com.manacommunity.api.finance.entity.GeneralLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GeneralLedgerEntryRepository extends JpaRepository<GeneralLedgerEntry, Long> {

    List<GeneralLedgerEntry> findByCommunityIdOrderByEntryDateDesc(Long communityId);

    @Query("SELECT COALESCE(SUM(e.credit), 0) FROM GeneralLedgerEntry e " +
            "WHERE e.communityId = :communityId AND e.accountCode LIKE '3%' " +
            "AND e.entryDate BETWEEN :from AND :to")
    BigDecimal sumIncomeCredits(Long communityId, LocalDate from, LocalDate to);

    @Query("SELECT COALESCE(SUM(e.debit), 0) FROM GeneralLedgerEntry e " +
            "WHERE e.communityId = :communityId AND e.accountCode LIKE '4%' " +
            "AND e.entryDate BETWEEN :from AND :to")
    BigDecimal sumExpenseDebits(Long communityId, LocalDate from, LocalDate to);
}
