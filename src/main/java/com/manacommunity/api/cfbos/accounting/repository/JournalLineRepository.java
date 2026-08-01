package com.manacommunity.api.cfbos.accounting.repository;

import com.manacommunity.api.cfbos.accounting.entity.Account;
import com.manacommunity.api.cfbos.accounting.entity.JournalLine;
import com.manacommunity.api.cfbos.accounting.enums.JournalEntryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface JournalLineRepository extends JpaRepository<JournalLine, Long> {
    @Query("SELECT jl FROM JournalLine jl JOIN jl.journalEntry je " +
           "WHERE jl.account = :account AND je.status = :status " +
           "AND je.entryDate BETWEEN :from AND :to ORDER BY je.entryDate")
    List<JournalLine> findByAccountAndStatusAndDateRange(Account account,
                                                          JournalEntryStatus status,
                                                          LocalDate from, LocalDate to);
}
