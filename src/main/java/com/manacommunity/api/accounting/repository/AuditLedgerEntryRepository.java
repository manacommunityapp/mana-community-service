package com.manacommunity.api.accounting.repository;

import com.manacommunity.api.accounting.entity.AuditLedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditLedgerEntryRepository extends JpaRepository<AuditLedgerEntryEntity, UUID> {

    Optional<AuditLedgerEntryEntity> findTopBySocietyIdOrderBySeqNoDesc(Long societyId);

    List<AuditLedgerEntryEntity> findBySocietyIdOrderBySeqNoAsc(Long societyId);
}
