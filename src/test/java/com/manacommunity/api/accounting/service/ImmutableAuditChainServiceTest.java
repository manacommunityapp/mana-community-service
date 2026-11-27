package com.manacommunity.api.accounting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.accounting.dto.AccountingDtos.*;
import com.manacommunity.api.accounting.entity.AuditLedgerEntryEntity;
import com.manacommunity.api.accounting.repository.AuditLedgerEntryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class ImmutableAuditChainServiceTest {

    @Test
    void chainsSha256EntriesAndDetectsTampering() {
        AuditLedgerEntryRepository repo = Mockito.mock(AuditLedgerEntryRepository.class);
        List<AuditLedgerEntryEntity> store = new ArrayList<>();

        when(repo.findTopBySocietyIdOrderBySeqNoDesc(1L))
                .thenAnswer(inv -> store.isEmpty() ? Optional.empty() : Optional.of(store.get(store.size() - 1)));
        when(repo.save(any(AuditLedgerEntryEntity.class))).thenAnswer(inv -> {
            AuditLedgerEntryEntity e = inv.getArgument(0);
            store.add(e);
            return e;
        });
        when(repo.findBySocietyIdOrderBySeqNoAsc(1L)).thenAnswer(inv -> store);

        ImmutableAuditChainService service = new ImmutableAuditChainService(repo, new ObjectMapper());

        AuditLedgerEntryDto e1 = service.postEntry(new PostAuditEntryRequest(
                1L, UUID.randomUUID().toString(), "MNT/26-27/00001", "Sales", "2026-11-01",
                "MYGATE_CLOUD", null, null,
                List.of(new LedgerLineDto("DR-101", "Flat A-402", 1_003_000L, 0L, "Towers", "Tower A"))
        ));
        AuditLedgerEntryDto e2 = service.postEntry(new PostAuditEntryRequest(
                1L, UUID.randomUUID().toString(), "MNT/26-27/00002", "Sales", "2026-11-02",
                "MYGATE_CLOUD", null, null,
                List.of(new LedgerLineDto("DR-102", "Flat A-403", 850_000L, 0L, "Towers", "Tower A"))
        ));

        assertEquals(ImmutableAuditChainService.GENESIS_HASH, e1.prevEntryHash());
        assertEquals(e1.entryHash(), e2.prevEntryHash());

        AuditChainVerificationDto ok = service.verifyChain(1L);
        assertTrue(ok.chainValid());
        assertEquals(2, ok.totalEntries());
        assertNotEquals(ImmutableAuditChainService.GENESIS_HASH, ok.merkleRoot());

        // Simulate silent DB tampering on entry #1
        store.get(0).setVoucherNumber("MNT/26-27/99999");
        AuditChainVerificationDto tampered = service.verifyChain(1L);
        assertFalse(tampered.chainValid());
        assertEquals(1L, tampered.brokenAtSeqNo());
    }
}
