package com.manacommunity.api.forensics.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.forensics.dto.ForensicsDtos.*;
import com.manacommunity.api.forensics.entity.ExternalMerkleAnchorEntity;
import com.manacommunity.api.forensics.entity.WormAuditEventEntity;
import com.manacommunity.api.forensics.repository.ExternalMerkleAnchorRepository;
import com.manacommunity.api.forensics.repository.WormAuditEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class WormAuditAnchorServiceTest {

    private WormAuditAnchorService createService(
            List<WormAuditEventEntity> events,
            List<ExternalMerkleAnchorEntity> anchors) {
        WormAuditEventRepository auditRepo = Mockito.mock(WormAuditEventRepository.class);
        ExternalMerkleAnchorRepository anchorRepo = Mockito.mock(ExternalMerkleAnchorRepository.class);

        when(auditRepo.findTopBySocietyIdOrderBySeqNoDesc(anyLong())).thenAnswer(inv -> {
            long sid = inv.getArgument(0);
            return events.stream()
                    .filter(e -> e.getSocietyId() == sid)
                    .max(Comparator.comparingLong(WormAuditEventEntity::getSeqNo));
        });
        when(auditRepo.findBySocietyIdAndSeqNo(anyLong(), anyLong())).thenAnswer(inv -> {
            long sid = inv.getArgument(0);
            long seq = inv.getArgument(1);
            return events.stream().filter(e -> e.getSocietyId() == sid && e.getSeqNo() == seq).findFirst();
        });
        when(auditRepo.findBySocietyIdOrderBySeqNoAsc(anyLong())).thenAnswer(inv -> {
            long sid = inv.getArgument(0);
            return events.stream()
                    .filter(e -> e.getSocietyId() == sid)
                    .sorted(Comparator.comparingLong(WormAuditEventEntity::getSeqNo))
                    .toList();
        });
        when(auditRepo.save(any())).thenAnswer(inv -> {
            WormAuditEventEntity e = inv.getArgument(0);
            events.removeIf(existing -> existing.getAuditId().equals(e.getAuditId()));
            events.add(e);
            return e;
        });
        doAnswer(inv -> {
            WormAuditEventEntity target = inv.getArgument(0);
            events.removeIf(e -> e.getAuditId().equals(target.getAuditId()));
            return null;
        }).when(auditRepo).delete(any());

        when(anchorRepo.findTopBySocietyIdOrderBySeqToDesc(anyLong())).thenAnswer(inv -> {
            long sid = inv.getArgument(0);
            return anchors.stream()
                    .filter(a -> a.getSocietyId() == sid)
                    .max(Comparator.comparingLong(ExternalMerkleAnchorEntity::getSeqTo));
        });
        when(anchorRepo.findBySocietyIdOrderBySeqToDesc(anyLong())).thenAnswer(inv -> {
            long sid = inv.getArgument(0);
            return anchors.stream()
                    .filter(a -> a.getSocietyId() == sid)
                    .sorted(Comparator.comparingLong(ExternalMerkleAnchorEntity::getSeqTo).reversed())
                    .toList();
        });
        when(anchorRepo.save(any())).thenAnswer(inv -> {
            ExternalMerkleAnchorEntity a = inv.getArgument(0);
            anchors.add(a);
            return a;
        });

        return new WormAuditAnchorService(auditRepo, anchorRepo);
    }

    @Test
    void enforcesDualControlOnPenaltyWaiverAndCreatesTombstoneOnVisitorDelete() {
        List<WormAuditEventEntity> events = new ArrayList<>();
        List<ExternalMerkleAnchorEntity> anchors = new ArrayList<>();
        WormAuditAnchorService service = createService(events, anchors);

        // Missing checker on ₹25,000 penalty waiver must fail
        assertThrows(InvalidInputException.class, () -> service.recordSensitiveAction(
                new RecordSensitiveAuditRequest(
                        1L, SensitiveActionType.PENALTY_WAIVER, "FLAT_LEDGER", "B-902",
                        101L, "President Anil", null, null, 2_500_000L,
                        "Waiving construction penalty", "{\"penalty\":25000}", "{\"penalty\":0}",
                        false, "10.1.0.5", "corr-1"
                )
        ));

        // Maker == Checker must fail
        assertThrows(InvalidInputException.class, () -> service.recordSensitiveAction(
                new RecordSensitiveAuditRequest(
                        1L, SensitiveActionType.PENALTY_WAIVER, "FLAT_LEDGER", "B-902",
                        101L, "President Anil", 101L, "President Anil", 2_500_000L,
                        "Waiving construction penalty", "{\"penalty\":25000}", "{\"penalty\":0}",
                        false, "10.1.0.5", "corr-1"
                )
        ));

        WormAuditEventDto waiver = service.recordSensitiveAction(
                new RecordSensitiveAuditRequest(
                        1L, SensitiveActionType.PENALTY_WAIVER, "FLAT_LEDGER", "B-902",
                        101L, "President Anil", 102L, "Treasurer Rao", 2_500_000L,
                        "AGM approved ₹25,000 penalty waiver", "{\"penalty\":25000}", "{\"penalty\":0}",
                        false, "10.1.0.5", "corr-2"
                )
        );
        assertEquals(AuditRiskTier.CRITICAL, waiver.riskTier());

        WormAuditEventDto visitorDel = service.recordSensitiveAction(
                new RecordSensitiveAuditRequest(
                        1L, SensitiveActionType.VISITOR_SOFT_DELETED, "VISITOR_LOG", "VIS-8821",
                        103L, "Security Officer", null, null, 0L,
                        "Duplicate entry at Gate 2", "{\"name\":\"Ramesh\"}", "{\"status\":\"TOMBSTONED\"}",
                        true, "10.1.0.9", "corr-3"
                )
        );
        assertTrue(visitorDel.tombstoned());
        assertEquals(2L, visitorDel.seqNo());
    }

    @Test
    void detectsRogueDbaTamperAndTailTruncationUsingExternalMerkleAnchor() {
        List<WormAuditEventEntity> events = new ArrayList<>();
        List<ExternalMerkleAnchorEntity> anchors = new ArrayList<>();
        WormAuditAnchorService service = createService(events, anchors);

        service.recordSensitiveAction(new RecordSensitiveAuditRequest(
                1L, SensitiveActionType.VENDOR_BLACKLIST, "VENDOR", "V-99",
                101L, "Admin A", 102L, "Treasurer B", 0L,
                "Fake GST invoice submitted", "{\"status\":\"ACTIVE\"}", "{\"status\":\"BLACKLISTED\"}",
                false, "10.0.0.2", "c-1"
        ));
        service.recordSensitiveAction(new RecordSensitiveAuditRequest(
                1L, SensitiveActionType.PENALTY_WAIVER, "FLAT_LEDGER", "B-902",
                101L, "Admin A", 102L, "Treasurer B", 2_500_000L,
                "Approved waiver", "{\"due\":25000}", "{\"due\":0}",
                false, "10.0.0.2", "c-2"
        ));

        ExternalMerkleAnchorDto anchor = service.anchorMerkleBatch(1L);
        assertEquals("COMPLIANCE", anchor.objectLockMode());
        assertEquals(1L, anchor.seqFrom());
        assertEquals(2L, anchor.seqTo());

        ForensicVerificationReportDto clean = service.verifyIntegrity(1L);
        assertTrue(clean.chainValid());
        assertTrue(clean.signaturesValid());
        assertTrue(clean.externalAnchorMatch());
        assertFalse(clean.tailTruncationDetected());

        // Simulate rogue DBA deleting the last row (tail truncation)
        ForensicVerificationReportDto afterTruncate = service.simulateRogueDbaTamper(
                new SimulateDbaTamperRequest(1L, "TRUNCATE_TAIL", 2L, null)
        );
        assertTrue(afterTruncate.tailTruncationDetected());
        assertFalse(afterTruncate.externalAnchorMatch());
        assertEquals(2L, afterTruncate.firstBrokenSeqNo());

        // Simulate rogue DBA altering payload on row #1
        ForensicVerificationReportDto afterEdit = service.simulateRogueDbaTamper(
                new SimulateDbaTamperRequest(1L, "MODIFY_ROW", 1L, "{\"status\":\"ACTIVE\",\"forged\":true}")
        );
        assertFalse(afterEdit.chainValid());
        assertEquals(1L, afterEdit.firstBrokenSeqNo());
    }
}
