package com.manacommunity.api.accounting.service;

import com.manacommunity.api.accounting.dto.AccountingDtos.*;
import com.manacommunity.api.accounting.entity.TallySyncConflictEntity;
import com.manacommunity.api.accounting.entity.TallySyncMapEntity;
import com.manacommunity.api.accounting.repository.TallySyncConflictRepository;
import com.manacommunity.api.accounting.repository.TallySyncMapRepository;
import com.manacommunity.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Conflict-safe bidirectional Tally XML synchronization engine preserving
 * deterministic GUIDs, voucher numbering series, Cost Centers, and audit hashes.
 */
@Service
@RequiredArgsConstructor
public class TallySyncService {

    private static final DateTimeFormatter TALLY_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final TallySyncMapRepository syncMapRepository;
    private final TallySyncConflictRepository conflictRepository;
    private final ImmutableAuditChainService auditChainService;

    @Transactional
    public TallyOutboxItemDto postAndEnqueueCloudVoucher(PostAuditEntryRequest req, String companyName) {
        AuditLedgerEntryDto entry = auditChainService.postEntry(req);
        UUID voucherUuid = UUID.fromString(entry.voucherId());
        UUID tallyGuid = deterministicGuid(req.societyId(), entry.voucherId());

        String xml = generateVoucherXml(
                companyName != null && !companyName.isBlank() ? companyName : "Mana Community Society",
                tallyGuid,
                entry.voucherNumber(),
                entry.voucherType(),
                LocalDate.parse(entry.postingDate()),
                entry.entryHash(),
                entry.lines()
        );
        String payloadHash = auditChainService.sha256Hex(auditChainService.canonicalJson(entry.lines()));

        TallySyncMapEntity syncMap = syncMapRepository
                .findBySocietyIdAndVoucherId(req.societyId(), voucherUuid)
                .orElseGet(TallySyncMapEntity::new);
        syncMap.setSocietyId(req.societyId());
        syncMap.setVoucherId(voucherUuid);
        syncMap.setTallyGuid(tallyGuid);
        syncMap.setTallyVoucherNumber(entry.voucherNumber());
        syncMap.setCloudEntryHash(entry.entryHash());
        syncMap.setSyncedPayloadHash(payloadHash);
        syncMap.setOwnerSource("MYGATE_CLOUD");
        syncMap.setSyncStatus("PENDING");
        syncMap.setTallyXml(xml);
        syncMapRepository.save(syncMap);

        return toOutboxDto(syncMap);
    }

    @Transactional(readOnly = true)
    public List<TallyOutboxItemDto> getPendingOutbox(long societyId) {
        return syncMapRepository.findBySocietyIdAndSyncStatusOrderByIdAsc(societyId, "PENDING")
                .stream()
                .map(this::toOutboxDto)
                .toList();
    }

    @Transactional
    public void acknowledgeOutbox(TallyAckRequest ack) {
        TallySyncMapEntity map = syncMapRepository
                .findBySocietyIdAndVoucherId(ack.societyId(), UUID.fromString(ack.voucherId()))
                .orElseThrow(() -> new ResourceNotFoundException("TallySyncMap", "voucherId", ack.voucherId()));
        map.setTallyAlterId(ack.tallyAlterId());
        map.setSyncStatus("SYNCED");
        map.setLastSyncedAt(Instant.now());
        syncMapRepository.save(map);
    }

    @Transactional
    public Map<String, Object> ingestInboundFromTally(TallyInboundSyncRequest req) {
        int imported = 0;
        int conflicts = 0;
        int unchanged = 0;

        for (TallyInboundVoucherDto v : req.vouchers()) {
            UUID guid = UUID.fromString(v.tallyGuid());
            String inboundPayloadHash = auditChainService.sha256Hex(auditChainService.canonicalJson(v.lines()));
            TallySyncMapEntity existing = syncMapRepository.findBySocietyIdAndTallyGuid(req.societyId(), guid).orElse(null);

            if (existing == null) {
                UUID newVoucherId = UUID.randomUUID();
                AuditLedgerEntryDto posted = auditChainService.postEntry(new PostAuditEntryRequest(
                        req.societyId(),
                        newVoucherId.toString(),
                        v.voucherNumber(),
                        v.voucherType() != null ? v.voucherType() : "Journal",
                        v.postingDate(),
                        "TALLY_CA",
                        null,
                        ImmutableAuditChainService.GENESIS_HASH,
                        v.lines()
                ));

                TallySyncMapEntity map = new TallySyncMapEntity();
                map.setSocietyId(req.societyId());
                map.setVoucherId(newVoucherId);
                map.setTallyGuid(guid);
                map.setTallyVoucherNumber(v.voucherNumber());
                map.setTallyAlterId(v.tallyAlterId());
                map.setCloudEntryHash(posted.entryHash());
                map.setSyncedPayloadHash(inboundPayloadHash);
                map.setOwnerSource("TALLY_CA");
                map.setSyncStatus("SYNCED");
                map.setLastSyncedAt(Instant.now());
                syncMapRepository.save(map);
                imported++;
                continue;
            }

            if (existing.getSyncedPayloadHash().equals(inboundPayloadHash)) {
                existing.setTallyAlterId(Math.max(existing.getTallyAlterId(), v.tallyAlterId()));
                existing.setLastSyncedAt(Instant.now());
                syncMapRepository.save(existing);
                unchanged++;
                continue;
            }

            // Cloud-owned voucher (or previously synced voucher) edited offline in Tally -> Quarantine conflict
            existing.setSyncStatus("CONFLICT");
            syncMapRepository.save(existing);

            TallySyncConflictEntity conflict = new TallySyncConflictEntity();
            conflict.setConflictId(UUID.randomUUID());
            conflict.setSocietyId(req.societyId());
            conflict.setVoucherId(existing.getVoucherId());
            conflict.setTallyGuid(guid);
            conflict.setCloudSnapshotJson(Map.of(
                    "voucherNumber", existing.getTallyVoucherNumber(),
                    "ownerSource", existing.getOwnerSource(),
                    "cloudEntryHash", existing.getCloudEntryHash(),
                    "syncedPayloadHash", existing.getSyncedPayloadHash()
            ));
            conflict.setTallySnapshotJson(Map.of(
                    "voucherNumber", v.voucherNumber(),
                    "tallyAlterId", v.tallyAlterId(),
                    "postingDate", v.postingDate(),
                    "inboundPayloadHash", inboundPayloadHash,
                    "lines", v.lines() != null ? v.lines() : List.of()
            ));
            conflict.setResolutionStatus("OPEN");
            conflictRepository.save(conflict);
            conflicts++;
        }

        return Map.of("imported", imported, "conflicts", conflicts, "unchanged", unchanged);
    }

    @Transactional(readOnly = true)
    public List<TallySyncConflictDto> listOpenConflicts(long societyId) {
        return conflictRepository.findBySocietyIdAndResolutionStatusOrderByCreatedAtDesc(societyId, "OPEN")
                .stream()
                .map(c -> new TallySyncConflictDto(
                        c.getConflictId().toString(),
                        c.getSocietyId(),
                        c.getVoucherId().toString(),
                        c.getTallyGuid().toString(),
                        c.getCloudSnapshotJson(),
                        c.getTallySnapshotJson(),
                        c.getResolutionStatus(),
                        c.getCreatedAt().toString()
                ))
                .toList();
    }

    @Transactional
    public void resolveConflict(String conflictId, ResolveConflictRequest req, Long resolvedByUserId) {
        TallySyncConflictEntity conflict = conflictRepository.findById(UUID.fromString(conflictId))
                .orElseThrow(() -> new ResourceNotFoundException("TallySyncConflict", "conflictId", conflictId));

        TallySyncMapEntity map = syncMapRepository
                .findBySocietyIdAndVoucherId(req.societyId(), conflict.getVoucherId())
                .orElseThrow(() -> new ResourceNotFoundException("TallySyncMap", "voucherId", conflict.getVoucherId().toString()));

        String action = req.resolutionAction() != null ? req.resolutionAction().toUpperCase() : "REVERT_TALLY";
        if ("REVERT_TALLY".equals(action)) {
            map.setSyncStatus("PENDING");
            syncMapRepository.save(map);
        } else {
            map.setSyncStatus("SYNCED");
            map.setLastSyncedAt(Instant.now());
            syncMapRepository.save(map);
        }

        conflict.setResolutionStatus(action);
        conflict.setResolvedByUserId(resolvedByUserId);
        conflict.setResolvedAt(Instant.now());
        conflictRepository.save(conflict);
    }

    @Transactional(readOnly = true)
    public TallySyncSummaryDto getSummary(long societyId) {
        return new TallySyncSummaryDto(
                societyId,
                syncMapRepository.countBySocietyIdAndSyncStatus(societyId, "PENDING"),
                syncMapRepository.countBySocietyIdAndSyncStatus(societyId, "SYNCED"),
                conflictRepository.countBySocietyIdAndResolutionStatus(societyId, "OPEN")
        );
    }

    public UUID deterministicGuid(long societyId, String voucherId) {
        return UUID.nameUUIDFromBytes((societyId + ":" + voucherId).getBytes(StandardCharsets.UTF_8));
    }

    public String generateVoucherXml(
            String companyName,
            UUID tallyGuid,
            String voucherNumber,
            String voucherType,
            LocalDate postingDate,
            String entryHash,
            List<LedgerLineDto> lines) {
        String dateStr = postingDate.format(TALLY_DATE);
        StringBuilder sb = new StringBuilder();
        sb.append("<ENVELOPE>\n")
          .append("  <HEADER><TALLYREQUEST>Import Data</TALLYREQUEST></HEADER>\n")
          .append("  <BODY><IMPORTDATA>\n")
          .append("    <REQUESTDESC><REPORTNAME>Vouchers</REPORTNAME><STATICVARIABLES><SVCURRENTCOMPANY>")
          .append(escapeXml(companyName))
          .append("</SVCURRENTCOMPANY></STATICVARIABLES></REQUESTDESC>\n")
          .append("    <REQUESTDATA><TALLYMESSAGE xmlns:UDF=\"TallyUDF\">\n")
          .append("      <VOUCHER REMOTEID=\"mygate-").append(tallyGuid).append("\" VCHTYPE=\"")
          .append(escapeXml(voucherType)).append("\" ACTION=\"Create\">\n")
          .append("        <GUID>").append(tallyGuid).append("</GUID>\n")
          .append("        <DATE>").append(dateStr).append("</DATE>\n")
          .append("        <VOUCHERTYPENAME>").append(escapeXml(voucherType)).append("</VOUCHERTYPENAME>\n")
          .append("        <VOUCHERNUMBER>").append(escapeXml(voucherNumber)).append("</VOUCHERNUMBER>\n")
          .append("        <NARRATION>MygateHash:").append(entryHash, 0, Math.min(16, entryHash.length())).append("</NARRATION>\n");

        if (lines != null) {
            for (LedgerLineDto line : lines) {
                boolean isDebit = line.debitPaise() > 0;
                long amountPaise = isDebit ? line.debitPaise() : line.creditPaise();
                String signedRupees = String.format(java.util.Locale.ROOT, "%.2f",
                        (isDebit ? -amountPaise : amountPaise) / 100.0);

                sb.append("        <ALLLEDGERENTRIES.LIST>\n")
                  .append("          <LEDGERNAME>").append(escapeXml(line.ledgerName())).append("</LEDGERNAME>\n")
                  .append("          <ISDEEMEDPOSITIVE>").append(isDebit ? "Yes" : "No").append("</ISDEEMEDPOSITIVE>\n")
                  .append("          <AMOUNT>").append(signedRupees).append("</AMOUNT>\n");

                if (line.costCenter() != null && !line.costCenter().isBlank()) {
                    String cat = line.costCategory() != null && !line.costCategory().isBlank()
                            ? line.costCategory() : "Society Cost Centers";
                    String absRupees = String.format(java.util.Locale.ROOT, "%.2f", amountPaise / 100.0);
                    sb.append("          <CATEGORYALLOCATIONS.LIST>\n")
                      .append("            <CATEGORY>").append(escapeXml(cat)).append("</CATEGORY>\n")
                      .append("            <COSTCENTREALLOCATIONS.LIST>\n")
                      .append("              <NAME>").append(escapeXml(line.costCenter())).append("</NAME>\n")
                      .append("              <AMOUNT>").append(absRupees).append("</AMOUNT>\n")
                      .append("            </COSTCENTREALLOCATIONS.LIST>\n")
                      .append("          </CATEGORYALLOCATIONS.LIST>\n");
                }
                sb.append("        </ALLLEDGERENTRIES.LIST>\n");
            }
        }

        sb.append("      </VOUCHER>\n")
          .append("    </TALLYMESSAGE></REQUESTDATA>\n")
          .append("  </IMPORTDATA></BODY>\n")
          .append("</ENVELOPE>");
        return sb.toString();
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private TallyOutboxItemDto toOutboxDto(TallySyncMapEntity m) {
        return new TallyOutboxItemDto(
                m.getVoucherId().toString(),
                m.getTallyGuid().toString(),
                m.getTallyVoucherNumber(),
                m.getCloudEntryHash(),
                m.getSyncedPayloadHash(),
                m.getSyncStatus(),
                m.getTallyXml()
        );
    }
}
