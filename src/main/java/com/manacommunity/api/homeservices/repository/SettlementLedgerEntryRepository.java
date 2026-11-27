package com.manacommunity.api.homeservices.repository;

import com.manacommunity.api.homeservices.entity.SettlementLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementLedgerEntryRepository extends JpaRepository<SettlementLedgerEntry, Long> {
    List<SettlementLedgerEntry> findBySettlementIdOrderByIdAsc(Long settlementId);
}
