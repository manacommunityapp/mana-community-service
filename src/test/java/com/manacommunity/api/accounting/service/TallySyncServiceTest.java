package com.manacommunity.api.accounting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.accounting.dto.AccountingDtos.*;
import com.manacommunity.api.accounting.entity.AuditLedgerEntryEntity;
import com.manacommunity.api.accounting.entity.TallySyncConflictEntity;
import com.manacommunity.api.accounting.entity.TallySyncMapEntity;
import com.manacommunity.api.accounting.repository.AuditLedgerEntryRepository;
import com.manacommunity.api.accounting.repository.TallySyncConflictRepository;
import com.manacommunity.api.accounting.repository.TallySyncMapRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TallySyncServiceTest {

    @Test
    void generatesTallyXmlWithCostCentersAndQuarantinesOfflineConflicts() {
        TallySyncMapRepository syncRepo = Mockito.mock(TallySyncMapRepository.class);
        TallySyncConflictRepository conflictRepo = Mockito.mock(TallySyncConflictRepository.class);
        AuditLedgerEntryRepository ledgerRepo = Mockito.mock(AuditLedgerEntryRepository.class);

        ImmutableAuditChainService auditService = new ImmutableAuditChainService(ledgerRepo, new ObjectMapper());
        TallySyncService tallyService = new TallySyncService(syncRepo, conflictRepo, auditService);

        UUID guid = tallyService.deterministicGuid(1L, "voucher-1");
        String xml = tallyService.generateVoucherXml(
                "Sunshine Residency CHS",
                guid,
                "MNT/26-27/00412",
                "Sales",
                LocalDate.of(2026, 11, 1),
                "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
                List.of(
                        new LedgerLineDto("4001", "Maintenance Income", 0L, 850_000L, "Society Towers", "Tower A"),
                        new LedgerLineDto("1101", "Flat A-402", 850_000L, 0L, null, null)
                )
        );
        assertTrue(xml.contains("<GUID>" + guid + "</GUID>"));
        assertTrue(xml.contains("<VOUCHERNUMBER>MNT/26-27/00412</VOUCHERNUMBER>"));
        assertTrue(xml.contains("<CATEGORY>Society Towers</CATEGORY>"));
        assertTrue(xml.contains("<NAME>Tower A</NAME>"));
        assertTrue(xml.contains("MygateHash:abcdef0123456789"));

        // Inbound: existing cloud voucher edited in Tally -> quarantined into conflict table
        TallySyncMapEntity existing = new TallySyncMapEntity();
        existing.setSocietyId(1L);
        existing.setVoucherId(UUID.randomUUID());
        existing.setTallyGuid(guid);
        existing.setTallyVoucherNumber("MNT/26-27/00412");
        existing.setCloudEntryHash("hash1");
        existing.setSyncedPayloadHash("original-payload-hash");
        existing.setOwnerSource("MYGATE_CLOUD");

        when(syncRepo.findBySocietyIdAndTallyGuid(1L, guid)).thenReturn(Optional.of(existing));

        Map<String, Object> result = tallyService.ingestInboundFromTally(new TallyInboundSyncRequest(
                1L,
                List.of(new TallyInboundVoucherDto(
                        guid.toString(),
                        "MNT/26-27/00412",
                        "Sales",
                        "2026-11-01",
                        99L,
                        "Edited by CA offline",
                        List.of(new LedgerLineDto("4001", "Maintenance Income", 0L, 900_000L, "Society Towers", "Tower A"))
                ))
        ));

        assertEquals(1, result.get("conflicts"));
        assertEquals("CONFLICT", existing.getSyncStatus());
        verify(conflictRepo, times(1)).save(any(TallySyncConflictEntity.class));
    }
}
