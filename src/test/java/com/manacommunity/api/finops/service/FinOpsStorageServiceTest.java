package com.manacommunity.api.finops.service;

import com.manacommunity.api.finops.dto.FinOpsDtos.*;
import com.manacommunity.api.finops.entity.MediaCompressionAuditEntity;
import com.manacommunity.api.finops.repository.MediaCompressionAuditRepository;
import com.manacommunity.api.finops.repository.StorageTierPolicyRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class FinOpsStorageServiceTest {

    @Test
    void enforces128KbTieringGuardrailAndTracksWebpSavings() {
        StorageTierPolicyRepository policyRepo = Mockito.mock(StorageTierPolicyRepository.class);
        MediaCompressionAuditRepository auditRepo = Mockito.mock(MediaCompressionAuditRepository.class);
        List<MediaCompressionAuditEntity> saved = new ArrayList<>();

        when(policyRepo.findAll()).thenReturn(List.of());
        when(auditRepo.save(any())).thenAnswer(inv -> {
            MediaCompressionAuditEntity e = inv.getArgument(0);
            saved.add(e);
            return e;
        });
        when(auditRepo.findBySocietyIdOrderByCreatedAtDesc(1L)).thenReturn(saved);

        FinOpsStorageService service = new FinOpsStorageService(policyRepo, auditRepo);

        // 2.8 MB raw JPEG -> 55 KB WebP (98% reduction; < 128 KB avoids S3 IA/Intelligent-Tiering trap)
        StorageFinOpsSummaryDto summary = service.recordCompressionAudit(new RecordCompressionAuditRequest(
                1L, "VISITOR_SNAPSHOT", 2_800_000L, 55_000L, "WEBP"
        ));

        assertEquals(1, summary.totalObjectsTracked());
        assertTrue(summary.compressionSavingsPct() > 97.0);
        assertEquals("S3_STANDARD_EXPIRE_180D", saved.get(0).getStorageTier());
        assertFalse(summary.policies().isEmpty());
        assertEquals(128, summary.policies().get(0).minBillableKb());
    }
}
