package com.manacommunity.api.repository;

import com.manacommunity.api.model.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    List<LedgerEntry> findByJournalId(String journalId);

    Page<LedgerEntry> findByCommunityIdOrderByPostedAtDesc(Long communityId, Pageable pageable);

    Page<LedgerEntry> findByAccountIdOrderByPostedAtDesc(Long accountId, Pageable pageable);

    @Query("SELECT SUM(CASE WHEN e.entryType = 'DEBIT' THEN e.amount ELSE 0 END) - " +
           "SUM(CASE WHEN e.entryType = 'CREDIT' THEN e.amount ELSE 0 END) " +
           "FROM LedgerEntry e WHERE e.account.id = :accountId AND e.isReversed = false")
    BigDecimal computeBalanceByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT SUM(CASE WHEN e.entryType = 'DEBIT' THEN e.amount ELSE 0 END) " +
           "FROM LedgerEntry e WHERE e.community.id = :communityId AND e.fiscalYear = :year AND e.isReversed = false")
    BigDecimal totalDebitsForYear(@Param("communityId") Long communityId, @Param("year") Integer year);

    @Query("SELECT SUM(CASE WHEN e.entryType = 'CREDIT' THEN e.amount ELSE 0 END) " +
           "FROM LedgerEntry e WHERE e.community.id = :communityId AND e.fiscalYear = :year AND e.isReversed = false")
    BigDecimal totalCreditsForYear(@Param("communityId") Long communityId, @Param("year") Integer year);

    List<LedgerEntry> findByCommunityIdAndPostedAtBetween(Long communityId, LocalDateTime from, LocalDateTime to);
}
