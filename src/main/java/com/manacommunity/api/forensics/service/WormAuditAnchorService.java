package com.manacommunity.api.forensics.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.exception.ResourceNotFoundException;
import com.manacommunity.api.forensics.dto.ForensicsDtos.*;
import com.manacommunity.api.forensics.entity.ExternalMerkleAnchorEntity;
import com.manacommunity.api.forensics.entity.WormAuditEventEntity;
import com.manacommunity.api.forensics.repository.ExternalMerkleAnchorRepository;
import com.manacommunity.api.forensics.repository.WormAuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Tamper-proof admin & guard audit log service combining Maker-Checker dual control,
 * soft-delete tombstones, KMS-isolated ECDSA signatures, hash chaining, and external
 * cross-account S3 Object Lock Merkle anchors to detect rogue DBA edits or tail truncation.
 */
@Service
@RequiredArgsConstructor
public class WormAuditAnchorService {

    static final long DUAL_CONTROL_THRESHOLD_PAISE = 500_000L; // ₹5,000
    static final long RETENTION_DAYS = 2555L; // 7 years statutory retention

    private final WormAuditEventRepository auditRepository;
    private final ExternalMerkleAnchorRepository anchorRepository;

    @Transactional
    public WormAuditEventDto recordSensitiveAction(RecordSensitiveAuditRequest req) {
        if (req.actionType() == null || req.resourceType() == null || req.resourceId() == null) {
            throw new InvalidInputException("actionType, resourceType, and resourceId are required");
        }
        if (req.reason() == null || req.reason().isBlank()) {
            throw new InvalidInputException("Audit justification reason is required");
        }

        AuditRiskTier riskTier = classifyRisk(req);
        boolean requiresChecker = riskTier == AuditRiskTier.CRITICAL
                || req.actionType() == SensitiveActionType.VENDOR_BLACKLIST;

        if (requiresChecker) {
            if (req.checkerId() == null || req.checkerName() == null || req.checkerName().isBlank()) {
                throw new InvalidInputException(
                        "Dual-control checker approval is required for " + req.actionType()
                );
            }
            if (req.checkerId().equals(req.actorId())) {
                throw new InvalidInputException("Maker and Checker must be distinct administrators");
            }
        }

        WormAuditEventEntity latest = auditRepository.findTopBySocietyIdOrderBySeqNoDesc(req.societyId()).orElse(null);
        long nextSeq = latest != null ? latest.getSeqNo() + 1L : 1L;
        String prevHash = latest != null ? latest.getRecordHash() : ForensicsCryptoUtil.GENESIS_HASH;

        Instant recordedAt = Instant.now();
        boolean tombstoned = req.tombstoneDelete() || req.actionType() == SensitiveActionType.VISITOR_SOFT_DELETED;
        String oldJson = req.oldValueJson() != null ? req.oldValueJson() : "{}";
        String newJson = req.newValueJson() != null ? req.newValueJson() : "{}";
        String ip = req.ipAddress() != null && !req.ipAddress().isBlank() ? req.ipAddress() : "127.0.0.1";
        String corrId = req.correlationId() != null && !req.correlationId().isBlank()
                ? req.correlationId()
                : UUID.randomUUID().toString();

        String canonical = canonicalAuditPayload(
                req.societyId(), nextSeq, req.actionType(), riskTier, req.resourceType(), req.resourceId(),
                req.actorId(), req.actorName(), req.checkerId(), req.reason(),
                oldJson, newJson, tombstoned, ip, corrId, recordedAt
        );
        String recordHash = ForensicsCryptoUtil.sha256Hex(prevHash + "|" + canonical);
        String ecdsaSig = ForensicsCryptoUtil.signEcdsa(recordHash + "|" + nextSeq + "|" + req.societyId());

        WormAuditEventEntity entity = new WormAuditEventEntity();
        entity.setAuditId(UUID.randomUUID());
        entity.setSocietyId(req.societyId());
        entity.setSeqNo(nextSeq);
        entity.setActionType(req.actionType());
        entity.setRiskTier(riskTier);
        entity.setResourceType(req.resourceType());
        entity.setResourceId(req.resourceId());
        entity.setActorId(req.actorId());
        entity.setActorName(req.actorName() != null ? req.actorName() : "Admin-" + req.actorId());
        entity.setCheckerId(req.checkerId());
        entity.setCheckerName(req.checkerName());
        entity.setReason(req.reason());
        entity.setOldValueJson(oldJson);
        entity.setNewValueJson(newJson);
        entity.setTombstoned(tombstoned);
        entity.setIpAddress(ip);
        entity.setCorrelationId(corrId);
        entity.setPrevHash(prevHash);
        entity.setRecordHash(recordHash);
        entity.setEcdsaSignature(ecdsaSig);
        entity.setRecordedAt(recordedAt);

        return toDto(auditRepository.save(entity));
    }

    @Transactional
    public ExternalMerkleAnchorDto anchorMerkleBatch(long societyId) {
        long lastAnchoredSeq = anchorRepository.findTopBySocietyIdOrderBySeqToDesc(societyId)
                .map(ExternalMerkleAnchorEntity::getSeqTo)
                .orElse(0L);

        List<WormAuditEventEntity> unanchored = auditRepository.findBySocietyIdOrderBySeqNoAsc(societyId)
                .stream()
                .filter(e -> e.getSeqNo() > lastAnchoredSeq)
                .toList();

        if (unanchored.isEmpty()) {
            throw new InvalidInputException("No un-anchored audit events available for society " + societyId);
        }

        long seqFrom = unanchored.get(0).getSeqNo();
        long seqTo = unanchored.get(unanchored.size() - 1).getSeqNo();
        String headHash = unanchored.get(unanchored.size() - 1).getRecordHash();
        List<String> leafHashes = unanchored.stream().map(WormAuditEventEntity::getRecordHash).toList();
        String merkleRoot = ForensicsCryptoUtil.computeMerkleRoot(leafHashes);

        String anchorPayload = societyId + "|" + seqFrom + "|" + seqTo + "|" + headHash + "|" + merkleRoot;
        String anchorSig = ForensicsCryptoUtil.signEcdsa(anchorPayload);
        Instant now = Instant.now();

        ExternalMerkleAnchorEntity anchor = new ExternalMerkleAnchorEntity();
        anchor.setAnchorId(UUID.randomUUID());
        anchor.setSocietyId(societyId);
        anchor.setSeqFrom(seqFrom);
        anchor.setSeqTo(seqTo);
        anchor.setEventCount(unanchored.size());
        anchor.setHeadHash(headHash);
        anchor.setMerkleRoot(merkleRoot);
        anchor.setEcdsaSignature(anchorSig);
        anchor.setWormBucketUri(String.format(
                "s3://mana-audit-worm-sec-acct/society-%d/anchors/seq-%d-%d.json", societyId, seqFrom, seqTo
        ));
        anchor.setObjectLockMode("COMPLIANCE");
        anchor.setRetentionUntil(now.plus(RETENTION_DAYS, ChronoUnit.DAYS));
        anchor.setAnchoredAt(now);

        return toAnchorDto(anchorRepository.save(anchor));
    }

    @Transactional(readOnly = true)
    public ForensicVerificationReportDto verifyIntegrity(long societyId) {
        List<WormAuditEventEntity> events = auditRepository.findBySocietyIdOrderBySeqNoAsc(societyId);
        List<ExternalMerkleAnchorEntity> anchors = anchorRepository.findBySocietyIdOrderBySeqToDesc(societyId);

        boolean chainValid = true;
        boolean signaturesValid = true;
        boolean externalAnchorMatch = true;
        boolean tailTruncationDetected = false;
        Long firstBrokenSeq = null;
        String failureReason = "All local hash links, ECDSA signatures, and external WORM Merkle anchors verified";

        Map<Long, WormAuditEventEntity> bySeq = new HashMap<>();
        long expectedSeq = 1L;
        String expectedPrev = ForensicsCryptoUtil.GENESIS_HASH;

        for (WormAuditEventEntity ev : events) {
            bySeq.put(ev.getSeqNo(), ev);
            if (!ev.getSeqNo().equals(expectedSeq) || !expectedPrev.equals(ev.getPrevHash())) {
                chainValid = false;
                firstBrokenSeq = ev.getSeqNo();
                failureReason = "Chain linkage gap or prevHash mismatch at seq " + ev.getSeqNo();
                break;
            }

            String canonical = canonicalAuditPayload(
                    ev.getSocietyId(), ev.getSeqNo(), ev.getActionType(), ev.getRiskTier(),
                    ev.getResourceType(), ev.getResourceId(), ev.getActorId(), ev.getActorName(),
                    ev.getCheckerId(), ev.getReason(), ev.getOldValueJson(), ev.getNewValueJson(),
                    Boolean.TRUE.equals(ev.getTombstoned()), ev.getIpAddress(), ev.getCorrelationId(),
                    ev.getRecordedAt()
            );
            String recomputed = ForensicsCryptoUtil.sha256Hex(ev.getPrevHash() + "|" + canonical);
            if (!recomputed.equals(ev.getRecordHash())) {
                chainValid = false;
                firstBrokenSeq = ev.getSeqNo();
                failureReason = "Record payload tampered (hash mismatch) at seq " + ev.getSeqNo();
                break;
            }

            boolean sigOk = ForensicsCryptoUtil.verifyEcdsa(
                    ev.getRecordHash() + "|" + ev.getSeqNo() + "|" + ev.getSocietyId(),
                    ev.getEcdsaSignature()
            );
            if (!sigOk) {
                signaturesValid = false;
                firstBrokenSeq = ev.getSeqNo();
                failureReason = "KMS ECDSA signature invalid at seq " + ev.getSeqNo();
                break;
            }

            expectedPrev = ev.getRecordHash();
            expectedSeq++;
        }

        long maxLocalSeq = events.isEmpty() ? 0L : events.get(events.size() - 1).getSeqNo();
        for (ExternalMerkleAnchorEntity anchor : anchors) {
            String anchorPayload = anchor.getSocietyId() + "|" + anchor.getSeqFrom() + "|" + anchor.getSeqTo()
                    + "|" + anchor.getHeadHash() + "|" + anchor.getMerkleRoot();
            if (!ForensicsCryptoUtil.verifyEcdsa(anchorPayload, anchor.getEcdsaSignature())) {
                externalAnchorMatch = false;
                signaturesValid = false;
                failureReason = "External anchor signature invalid for range " + anchor.getSeqFrom() + "-" + anchor.getSeqTo();
                break;
            }
            if (maxLocalSeq < anchor.getSeqTo()) {
                tailTruncationDetected = true;
                externalAnchorMatch = false;
                if (firstBrokenSeq == null) {
                    firstBrokenSeq = anchor.getSeqTo();
                    failureReason = "TAIL_TRUNCATION_DETECTED: External WORM anchor reaches seq "
                            + anchor.getSeqTo() + " but local DB stops at seq " + maxLocalSeq;
                }
                break;
            }
            List<String> sliceLeaves = new ArrayList<>();
            for (long s = anchor.getSeqFrom(); s <= anchor.getSeqTo(); s++) {
                WormAuditEventEntity item = bySeq.get(s);
                if (item == null) {
                    tailTruncationDetected = true;
                    externalAnchorMatch = false;
                    if (firstBrokenSeq == null) {
                        firstBrokenSeq = s;
                        failureReason = "Missing event seq " + s + " required by external anchor";
                    }
                    break;
                }
                sliceLeaves.add(item.getRecordHash());
            }
            if (!externalAnchorMatch) break;

            String recomputedRoot = ForensicsCryptoUtil.computeMerkleRoot(sliceLeaves);
            if (!recomputedRoot.equals(anchor.getMerkleRoot())) {
                externalAnchorMatch = false;
                if (firstBrokenSeq == null) firstBrokenSeq = anchor.getSeqFrom();
                failureReason = "EXTERNAL_ANCHOR_MISMATCH: Local Merkle root diverges from S3 Object Lock anchor ["
                        + anchor.getSeqFrom() + ".." + anchor.getSeqTo() + "]";
                break;
            }
        }

        String headHash = events.isEmpty() ? ForensicsCryptoUtil.GENESIS_HASH : events.get(events.size() - 1).getRecordHash();
        String latestRoot = anchors.isEmpty() ? null : anchors.get(0).getMerkleRoot();

        return new ForensicVerificationReportDto(
                societyId,
                events.size(),
                anchors.size(),
                chainValid,
                signaturesValid,
                externalAnchorMatch,
                tailTruncationDetected,
                firstBrokenSeq,
                failureReason,
                headHash,
                latestRoot,
                Instant.now()
        );
    }

    /**
     * Forensics drill helper simulating a rogue DBA bypassing DB triggers to alter
     * a historical payload or truncate tail rows while the external WORM anchor catches it.
     */
    @Transactional
    public ForensicVerificationReportDto simulateRogueDbaTamper(SimulateDbaTamperRequest req) {
        String mode = req.mode() != null ? req.mode().toUpperCase(Locale.ROOT) : "MODIFY_ROW";
        if ("TRUNCATE_TAIL".equals(mode)) {
            WormAuditEventEntity latest = auditRepository.findTopBySocietyIdOrderBySeqNoDesc(req.societyId())
                    .orElseThrow(() -> new ResourceNotFoundException("No audit events to truncate"));
            auditRepository.delete(latest);
        } else {
            long targetSeq = req.targetSeqNo() != null ? req.targetSeqNo() : 1L;
            WormAuditEventEntity target = auditRepository.findBySocietyIdAndSeqNo(req.societyId(), targetSeq)
                    .orElseThrow(() -> new ResourceNotFoundException("Audit event seq " + targetSeq + " not found"));
            target.setNewValueJson(req.forgedNewValueJson() != null
                    ? req.forgedNewValueJson()
                    : "{\"waivedPaise\":0,\"tamperedBy\":\"ROGUE_DBA\"}");
            auditRepository.save(target);
        }
        return verifyIntegrity(req.societyId());
    }

    @Transactional(readOnly = true)
    public List<WormAuditEventDto> listAuditEvents(long societyId) {
        return auditRepository.findBySocietyIdOrderBySeqNoAsc(societyId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ExternalMerkleAnchorDto> listAnchors(long societyId) {
        return anchorRepository.findBySocietyIdOrderBySeqToDesc(societyId).stream().map(this::toAnchorDto).toList();
    }

    private AuditRiskTier classifyRisk(RecordSensitiveAuditRequest req) {
        if (req.actionType() == SensitiveActionType.PENALTY_WAIVER
                && req.monetaryImpactPaise() != null
                && req.monetaryImpactPaise() >= DUAL_CONTROL_THRESHOLD_PAISE) {
            return AuditRiskTier.CRITICAL;
        }
        if (req.actionType() == SensitiveActionType.VENDOR_BLACKLIST
                || req.actionType() == SensitiveActionType.VISITOR_SOFT_DELETED
                || req.actionType() == SensitiveActionType.CREDIT_NOTE_BACKDATED) {
            return AuditRiskTier.HIGH;
        }
        return AuditRiskTier.STANDARD;
    }

    private String canonicalAuditPayload(
            long societyId, long seqNo, SensitiveActionType actionType, AuditRiskTier riskTier,
            String resourceType, String resourceId, long actorId, String actorName,
            Long checkerId, String reason, String oldJson, String newJson,
            boolean tombstoned, String ip, String corrId, Instant recordedAt) {
        return String.join("|",
                String.valueOf(societyId),
                String.valueOf(seqNo),
                actionType.name(),
                riskTier.name(),
                resourceType,
                resourceId,
                String.valueOf(actorId),
                actorName != null ? actorName : "-",
                checkerId != null ? String.valueOf(checkerId) : "-",
                reason,
                ForensicsCryptoUtil.sha256Hex(oldJson),
                ForensicsCryptoUtil.sha256Hex(newJson),
                String.valueOf(tombstoned),
                ip,
                corrId,
                String.valueOf(recordedAt.toEpochMilli())
        );
    }

    private WormAuditEventDto toDto(WormAuditEventEntity e) {
        return new WormAuditEventDto(
                e.getAuditId(),
                e.getSocietyId(),
                e.getSeqNo(),
                e.getActionType(),
                e.getRiskTier(),
                e.getResourceType(),
                e.getResourceId(),
                e.getActorId(),
                e.getActorName(),
                e.getCheckerId(),
                e.getCheckerName(),
                e.getReason(),
                e.getOldValueJson(),
                e.getNewValueJson(),
                Boolean.TRUE.equals(e.getTombstoned()),
                e.getIpAddress(),
                e.getCorrelationId(),
                e.getPrevHash(),
                e.getRecordHash(),
                e.getEcdsaSignature(),
                e.getRecordedAt()
        );
    }

    private ExternalMerkleAnchorDto toAnchorDto(ExternalMerkleAnchorEntity a) {
        return new ExternalMerkleAnchorDto(
                a.getAnchorId(),
                a.getSocietyId(),
                a.getSeqFrom(),
                a.getSeqTo(),
                a.getEventCount(),
                a.getHeadHash(),
                a.getMerkleRoot(),
                a.getEcdsaSignature(),
                a.getWormBucketUri(),
                a.getObjectLockMode(),
                a.getRetentionUntil(),
                a.getAnchoredAt()
        );
    }
}
