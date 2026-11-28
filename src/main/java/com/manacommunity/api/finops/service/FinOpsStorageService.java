package com.manacommunity.api.finops.service;

import com.manacommunity.api.finops.dto.FinOpsDtos.*;
import com.manacommunity.api.finops.entity.MediaCompressionAuditEntity;
import com.manacommunity.api.finops.entity.StorageTierPolicyEntity;
import com.manacommunity.api.finops.repository.MediaCompressionAuditRepository;
import com.manacommunity.api.finops.repository.StorageTierPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinOpsStorageService {

    private final StorageTierPolicyRepository policyRepository;
    private final MediaCompressionAuditRepository auditRepository;

    @Transactional(readOnly = true)
    public List<StorageTierPolicyDto> listPolicies() {
        List<StorageTierPolicyEntity> stored = policyRepository.findAll();
        if (!stored.isEmpty()) {
            return stored.stream().map(this::toPolicyDto).toList();
        }
        return defaultPolicies();
    }

    @Transactional
    public StorageFinOpsSummaryDto recordCompressionAudit(RecordCompressionAuditRequest req) {
        MediaCompressionAuditEntity audit = new MediaCompressionAuditEntity();
        audit.setAuditId(UUID.randomUUID());
        audit.setSocietyId(req.societyId());
        audit.setAssetClass(req.assetClass());
        audit.setOriginalBytes(Math.max(1L, req.originalBytes()));
        audit.setCompressedBytes(Math.max(1L, req.compressedBytes()));
        audit.setFormat(req.format() != null ? req.format() : "WEBP");
        // Guardrail: objects < 128 KB stay out of S3 Standard-IA / Intelligent-Tiering per AWS min billable size
        audit.setStorageTier(req.compressedBytes() >= 128 * 1024 ? "S3_STANDARD_TO_GLACIER_IR" : "S3_STANDARD_EXPIRE_180D");
        auditRepository.save(audit);
        return getStorageSummary(req.societyId());
    }

    @Transactional(readOnly = true)
    public StorageFinOpsSummaryDto getStorageSummary(long societyId) {
        List<MediaCompressionAuditEntity> audits = auditRepository.findBySocietyIdOrderByCreatedAtDesc(societyId);
        long orig = audits.stream().mapToLong(MediaCompressionAuditEntity::getOriginalBytes).sum();
        long comp = audits.stream().mapToLong(MediaCompressionAuditEntity::getCompressedBytes).sum();
        double savingsPct = orig > 0 ? ((orig - comp) * 100.0) / orig : 0.0;

        // S3 Standard ~ ₹1.95/GB/mo (~0.195 paise/MB) -> convert saved GB into paise/month
        double savedGb = Math.max(0.0, (orig - comp) / (1024.0 * 1024.0 * 1024.0));
        long monthlySavedPaise = Math.round(savedGb * 195.0);

        return new StorageFinOpsSummaryDto(
                societyId,
                audits.size(),
                orig,
                comp,
                savingsPct,
                monthlySavedPaise,
                listPolicies()
        );
    }

    private List<StorageTierPolicyDto> defaultPolicies() {
        return List.of(
                new StorageTierPolicyDto(
                        "VISITOR_SNAPSHOT", "WEBP_Q75",
                        "S3_STANDARD", 7, "S3_BUNDLED_CAR_IA", 30, "GLACIER_INSTANT_RETRIEVAL", 180, 128,
                        "Objects < 128 KB avoid Intelligent-Tiering monitoring tax; bundled or expired at 180d per DPDP"
                ),
                new StorageTierPolicyDto(
                        "ANPR_PLATE_CROP", "WEBP_Q80",
                        "S3_STANDARD", 7, "S3_BUNDLED_CAR_IA", 30, "GLACIER_INSTANT_RETRIEVAL", 180, 128,
                        "960px WebP + CloudFront Origin Shield cache (1-year immutable)"
                ),
                new StorageTierPolicyDto(
                        "STATUTORY_PDF_INVOICE", "PDF_STREAM_OPT",
                        "S3_STANDARD", 30, "S3_STANDARD_IA", 365, "GLACIER_INSTANT_RETRIEVAL", 2920, 128,
                        "8-year statutory retention with millisecond Glacier IR audit retrieval"
                )
        );
    }

    private StorageTierPolicyDto toPolicyDto(StorageTierPolicyEntity e) {
        return new StorageTierPolicyDto(
                e.getAssetClass(), e.getTargetFormat(), e.getHotTier(), e.getHotDays(),
                e.getWarmTier(), e.getWarmDays(), e.getArchiveTier(), e.getRetentionDays(),
                e.getMinBillableKb(), e.getNotes()
        );
    }
}
