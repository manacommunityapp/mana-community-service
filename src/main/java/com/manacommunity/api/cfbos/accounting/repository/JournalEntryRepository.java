package com.manacommunity.api.cfbos.accounting.repository;

import com.manacommunity.api.cfbos.accounting.entity.JournalEntry;
import com.manacommunity.api.cfbos.accounting.enums.JournalEntryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {
    Page<JournalEntry> findByStatusAndEntryDateBetween(JournalEntryStatus status,
                                                        LocalDate from, LocalDate to,
                                                        Pageable pageable);
    Page<JournalEntry> findByEntryDateBetween(LocalDate from, LocalDate to, Pageable pageable);
}
