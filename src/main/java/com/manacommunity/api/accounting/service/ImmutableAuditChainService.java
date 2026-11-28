package com.manacommunity.api.accounting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.manacommunity.api.accounting.dto.AccountingDtos.*;
import com.manacommunity.api.accounting.entity.AuditLedgerEntryEntity;
import com.manacommunity.api.accounting.repository.AuditLedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Append-only, cryptographically hash-chained accounting ledger with Merkle root verification.
 */
@Service
@RequiredArgsConstructor
public class ImmutableAuditChainService {

    public static final String GENESIS_HASH = "0".repeat(64);

    private final AuditLedgerEntryRepository ledgerRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public AuditLedgerEntryDto postEntry(PostAuditEntryRequest req) {
        AuditLedgerEntryEntity prev = ledgerRepository.findTopBySocietyIdOrderBySeqNoDesc(req.societyId())
                .orElse(null);

        long nextSeq = prev != null ? prev.getSeqNo() + 1L : 1L;
        String prevHash = prev != null ? prev.getEntryHash() : GENESIS_HASH;
        UUID voucherUuid = req.voucherId() != null && !req.voucherId().isBlank()
                ? UUID.fromString(req.voucherId())
                : UUID.randomUUID();
        LocalDate postingDate = LocalDate.parse(req.postingDate());
        String sourceSystem = req.sourceSystem() != null ? req.sourceSystem() : "MYGATE_CLOUD";
        String taxSha = req.taxSnapshotSha256() != null && req.taxSnapshotSha256().length() == 64
                ? req.taxSnapshotSha256()
                : GENESIS_HASH;

        List<LedgerLineDto> sortedLines = req.lines() == null ? List.of() : req.lines().stream()
                .sorted(Comparator.comparing(LedgerLineDto::ledgerCode)
                        .thenComparing(l -> l.costCenter() != null ? l.costCenter() : ""))
                .toList();

        String canonicalPayload = buildCanonicalPayload(
                req.societyId(),
                nextSeq,
                voucherUuid,
                req.voucherNumber(),
                req.voucherType(),
                postingDate,
                sourceSystem,
                taxSha,
                sortedLines
        );
        String entryHash = sha256Hex(prevHash + ":" + canonicalPayload);

        AuditLedgerEntryEntity entity = new AuditLedgerEntryEntity();
        entity.setEntryId(UUID.randomUUID());
        entity.setSocietyId(req.societyId());
        entity.setSeqNo(nextSeq);
        entity.setVoucherId(voucherUuid);
        entity.setVoucherNumber(req.voucherNumber());
        entity.setVoucherType(req.voucherType());
        entity.setPostingDate(postingDate);
        entity.setSourceSystem(sourceSystem);
        if (req.reversesEntryId() != null && !req.reversesEntryId().isBlank()) {
            entity.setReversesEntryId(UUID.fromString(req.reversesEntryId()));
        }
        entity.setCanonicalLinesJson(sortedLines);
        entity.setTaxSnapshotSha256(taxSha);
        entity.setPrevEntryHash(prevHash);
        entity.setEntryHash(entryHash);
        ledgerRepository.save(entity);

        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public List<AuditLedgerEntryDto> listEntries(long societyId) {
        return ledgerRepository.findBySocietyIdOrderBySeqNoAsc(societyId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public AuditChainVerificationDto verifyChain(long societyId) {
        List<AuditLedgerEntryEntity> entries = ledgerRepository.findBySocietyIdOrderBySeqNoAsc(societyId);
        if (entries.isEmpty()) {
            return new AuditChainVerificationDto(societyId, 0, true, GENESIS_HASH, null, "Empty ledger chain");
        }

        String expectedPrev = GENESIS_HASH;
        long expectedSeq = 1L;
        List<String> leafHashes = new ArrayList<>(entries.size());

        for (AuditLedgerEntryEntity e : entries) {
            if (e.getSeqNo() != expectedSeq || !expectedPrev.equals(e.getPrevEntryHash())) {
                return new AuditChainVerificationDto(
                        societyId, entries.size(), false, computeMerkleRoot(leafHashes),
                        e.getSeqNo(), "Chain linkage mismatch at seq " + e.getSeqNo()
                );
            }

            List<LedgerLineDto> sortedLines = e.getCanonicalLinesJson() == null ? List.of()
                    : e.getCanonicalLinesJson().stream()
                    .sorted(Comparator.comparing(LedgerLineDto::ledgerCode)
                            .thenComparing(l -> l.costCenter() != null ? l.costCenter() : ""))
                    .toList();

            String canonical = buildCanonicalPayload(
                    e.getSocietyId(),
                    e.getSeqNo(),
                    e.getVoucherId(),
                    e.getVoucherNumber(),
                    e.getVoucherType(),
                    e.getPostingDate(),
                    e.getSourceSystem(),
                    e.getTaxSnapshotSha256(),
                    sortedLines
            );
            String recomputed = sha256Hex(e.getPrevEntryHash() + ":" + canonical);
            if (!recomputed.equals(e.getEntryHash())) {
                return new AuditChainVerificationDto(
                        societyId, entries.size(), false, computeMerkleRoot(leafHashes),
                        e.getSeqNo(), "Entry hash tamper detected at seq " + e.getSeqNo()
                );
            }

            leafHashes.add(e.getEntryHash());
            expectedPrev = e.getEntryHash();
            expectedSeq++;
        }

        return new AuditChainVerificationDto(
                societyId,
                entries.size(),
                true,
                computeMerkleRoot(leafHashes),
                null,
                "All " + entries.size() + " entries verified against SHA-256 chain & Merkle root"
        );
    }

    public String computeMerkleRoot(List<String> hashes) {
        if (hashes == null || hashes.isEmpty()) {
            return GENESIS_HASH;
        }
        List<String> level = new ArrayList<>(hashes);
        while (level.size() > 1) {
            List<String> next = new ArrayList<>((level.size() + 1) / 2);
            for (int i = 0; i < level.size(); i += 2) {
                String left = level.get(i);
                String right = (i + 1 < level.size()) ? level.get(i + 1) : left;
                next.add(sha256Hex(left + right));
            }
            level = next;
        }
        return level.get(0);
    }

    public String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    public String canonicalJson(Object value) {
        try {
            ObjectMapper sorted = objectMapper.copy().configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
            return sorted.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Canonical JSON serialization failed", ex);
        }
    }

    private String buildCanonicalPayload(
            long societyId,
            long seqNo,
            UUID voucherId,
            String voucherNumber,
            String voucherType,
            LocalDate postingDate,
            String sourceSystem,
            String taxSnapshotSha256,
            List<LedgerLineDto> lines) {
        return societyId + "|" + seqNo + "|" + voucherId + "|" + voucherNumber + "|"
                + voucherType + "|" + postingDate + "|" + sourceSystem + "|"
                + taxSnapshotSha256 + "|" + canonicalJson(lines);
    }

    private AuditLedgerEntryDto toDto(AuditLedgerEntryEntity e) {
        return new AuditLedgerEntryDto(
                e.getEntryId().toString(),
                e.getSocietyId(),
                e.getSeqNo(),
                e.getVoucherId().toString(),
                e.getVoucherNumber(),
                e.getVoucherType(),
                e.getPostingDate().toString(),
                e.getSourceSystem(),
                e.getReversesEntryId() != null ? e.getReversesEntryId().toString() : null,
                e.getCanonicalLinesJson(),
                e.getTaxSnapshotSha256(),
                e.getPrevEntryHash(),
                e.getEntryHash(),
                e.getCreatedAt().toString()
        );
    }
}
