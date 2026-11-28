package com.manacommunity.api.forensics.service;

import com.manacommunity.api.exception.InvalidInputException;
import com.manacommunity.api.forensics.dto.ForensicsDtos.*;
import com.manacommunity.api.forensics.entity.BitemporalLedgerEventEntity;
import com.manacommunity.api.forensics.entity.LedgerCheckpointEntity;
import com.manacommunity.api.forensics.repository.BitemporalLedgerEventRepository;
import com.manacommunity.api.forensics.repository.LedgerCheckpointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Append-only bitemporal event-sourced accounting ledger supporting exact point-in-time
 * reconstruction (recorded_at cutoff) vs. restated period balances (effective_date cutoff)
 * accelerated by monthly checkpoint snapshots.
 */
@Service
@RequiredArgsConstructor
public class BitemporalLedgerService {

    static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final BitemporalLedgerEventRepository eventRepository;
    private final LedgerCheckpointRepository checkpointRepository;

    @Transactional
    public BitemporalLedgerEventDto appendEvent(AppendLedgerEventRequest req) {
        if (req.idempotencyKey() == null || req.idempotencyKey().isBlank()) {
            throw new InvalidInputException("idempotencyKey is required");
        }
        Optional<BitemporalLedgerEventEntity> existing =
                eventRepository.findBySocietyIdAndIdempotencyKey(req.societyId(), req.idempotencyKey());
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        if (req.unitCode() == null || req.unitCode().isBlank()) {
            throw new InvalidInputException("unitCode is required");
        }
        if (req.eventType() == null || req.effectiveDate() == null) {
            throw new InvalidInputException("eventType and effectiveDate are required");
        }

        long nextStreamVersion = eventRepository
                .findTopBySocietyIdAndFlatIdOrderByStreamVersionDesc(req.societyId(), req.flatId())
                .map(e -> e.getStreamVersion() + 1L)
                .orElse(1L);

        BitemporalLedgerEventEntity latestSocietyEvent = eventRepository
                .findTopBySocietyIdOrderByGlobalSeqDesc(req.societyId())
                .orElse(null);
        long nextGlobalSeq = latestSocietyEvent != null ? latestSocietyEvent.getGlobalSeq() + 1L : 1L;
        String prevHash = latestSocietyEvent != null ? latestSocietyEvent.getEventHash() : ForensicsCryptoUtil.GENESIS_HASH;

        Instant recordedAt = req.recordedAt() != null ? req.recordedAt() : Instant.now();
        String linesJson = req.linesJson() != null && !req.linesJson().isBlank()
                ? req.linesJson()
                : "{\"signedAmountPaise\":" + req.signedAmountPaise() + "}";
        String reason = req.reasonCode() != null ? req.reasonCode() : req.eventType().name();
        String actor = req.actorName() != null && !req.actorName().isBlank() ? req.actorName() : "SYSTEM_BILLING";

        String canonical = String.join("|",
                String.valueOf(req.societyId()),
                String.valueOf(req.flatId()),
                String.valueOf(nextStreamVersion),
                String.valueOf(nextGlobalSeq),
                req.eventType().name(),
                req.effectiveDate().toString(),
                String.valueOf(recordedAt.toEpochMilli()),
                String.valueOf(req.signedAmountPaise()),
                req.causesEventId() != null ? req.causesEventId().toString() : "-",
                reason,
                linesJson
        );
        String eventHash = ForensicsCryptoUtil.sha256Hex(prevHash + "|" + canonical);

        BitemporalLedgerEventEntity entity = new BitemporalLedgerEventEntity();
        entity.setEventId(UUID.randomUUID());
        entity.setSocietyId(req.societyId());
        entity.setFlatId(req.flatId());
        entity.setUnitCode(req.unitCode());
        entity.setStreamVersion(nextStreamVersion);
        entity.setGlobalSeq(nextGlobalSeq);
        entity.setEventType(req.eventType());
        entity.setEffectiveDate(req.effectiveDate());
        entity.setRecordedAt(recordedAt);
        entity.setSignedAmountPaise(req.signedAmountPaise());
        entity.setCausesEventId(req.causesEventId());
        entity.setReasonCode(reason);
        entity.setActorName(actor);
        entity.setIdempotencyKey(req.idempotencyKey());
        entity.setLinesJson(linesJson);
        entity.setPrevEventHash(prevHash);
        entity.setEventHash(eventHash);

        return toDto(eventRepository.save(entity));
    }

    @Transactional
    public LedgerCheckpointDto createCheckpoint(long societyId, long flatId, LocalDate checkpointDate, Instant recordedCutoff) {
        List<BitemporalLedgerEventEntity> all = eventRepository
                .findBySocietyIdAndFlatIdOrderByStreamVersionAsc(societyId, flatId);
        if (all.isEmpty()) {
            throw new InvalidInputException("No ledger events exist for flat " + flatId);
        }

        Instant effectiveCutoff = recordedCutoff != null ? recordedCutoff : Instant.now();
        long balance = 0L;
        long maxStreamVersion = 0L;
        String lastHash = ForensicsCryptoUtil.GENESIS_HASH;
        String unitCode = all.get(0).getUnitCode();

        for (BitemporalLedgerEventEntity ev : all) {
            if (!ev.getRecordedAt().isAfter(effectiveCutoff)) {
                maxStreamVersion = ev.getStreamVersion();
                lastHash = ev.getEventHash();
                if (!ev.getEffectiveDate().isAfter(checkpointDate)) {
                    balance += ev.getSignedAmountPaise();
                }
            }
        }

        LedgerCheckpointEntity cp = new LedgerCheckpointEntity();
        cp.setCheckpointId(UUID.randomUUID());
        cp.setSocietyId(societyId);
        cp.setFlatId(flatId);
        cp.setUnitCode(unitCode);
        cp.setCheckpointDate(checkpointDate);
        cp.setRecordedCutoff(effectiveCutoff);
        cp.setStreamVersion(maxStreamVersion);
        cp.setBalancePaise(balance);
        cp.setStateHash(ForensicsCryptoUtil.sha256Hex(societyId + "|" + flatId + "|" + maxStreamVersion + "|" + balance + "|" + lastHash));
        cp.setCreatedAt(Instant.now());

        return toCheckpointDto(checkpointRepository.save(cp));
    }

    @Transactional(readOnly = true)
    public TimeTravelReconstructionDto reconstructAtPointInTime(TimeTravelQueryRequest req) {
        LocalDate asOfEffectiveDate = req.asOfEffectiveDate() != null
                ? req.asOfEffectiveDate()
                : LocalDate.now(IST);
        Instant asOfRecordedAt = req.asOfRecordedAt() != null
                ? req.asOfRecordedAt()
                : Instant.now();

        List<BitemporalLedgerEventEntity> all = eventRepository
                .findBySocietyIdAndFlatIdOrderByStreamVersionAsc(req.societyId(), req.flatId());
        String unitCode = all.isEmpty() ? "Flat-" + req.flatId() : all.get(0).getUnitCode();

        LedgerCheckpointEntity checkpoint = checkpointRepository
                .findTopBySocietyIdAndFlatIdAndRecordedCutoffLessThanEqualOrderByStreamVersionDesc(
                        req.societyId(), req.flatId(), asOfRecordedAt)
                .filter(cp -> !cp.getCheckpointDate().isAfter(asOfEffectiveDate))
                .orElse(null);

        long asKnownBalance = checkpoint != null ? checkpoint.getBalancePaise() : 0L;
        int replayedCount = 0;
        List<BitemporalLedgerEventDto> asKnownTimeline = new ArrayList<>();
        List<BitemporalLedgerEventDto> retroactiveAdjustments = new ArrayList<>();
        long restatedBalance = 0L;
        long liveBalance = 0L;

        for (BitemporalLedgerEventEntity ev : all) {
            liveBalance += ev.getSignedAmountPaise();

            boolean withinEffective = !ev.getEffectiveDate().isAfter(asOfEffectiveDate);
            boolean withinRecorded = !ev.getRecordedAt().isAfter(asOfRecordedAt);

            if (withinEffective) {
                restatedBalance += ev.getSignedAmountPaise();
                if (!withinRecorded) {
                    retroactiveAdjustments.add(toDto(ev));
                }
            }

            if (withinEffective && withinRecorded) {
                asKnownTimeline.add(toDto(ev));
                if (checkpoint == null) {
                    asKnownBalance += ev.getSignedAmountPaise();
                    replayedCount++;
                } else if (ev.getStreamVersion() > checkpoint.getStreamVersion()
                        || (ev.getEffectiveDate().isAfter(checkpoint.getCheckpointDate())
                        && !ev.getRecordedAt().isAfter(checkpoint.getRecordedCutoff()))) {
                    asKnownBalance += ev.getSignedAmountPaise();
                    replayedCount++;
                }
            }
        }

        return new TimeTravelReconstructionDto(
                req.societyId(),
                req.flatId(),
                unitCode,
                asOfEffectiveDate,
                asOfRecordedAt,
                asKnownBalance,
                restatedBalance,
                liveBalance,
                restatedBalance - asKnownBalance,
                checkpoint != null ? toCheckpointDto(checkpoint) : null,
                replayedCount,
                asKnownTimeline,
                retroactiveAdjustments
        );
    }

    @Transactional(readOnly = true)
    public List<BitemporalLedgerEventDto> listFlatEvents(long societyId, long flatId) {
        return eventRepository.findBySocietyIdAndFlatIdOrderByStreamVersionAsc(societyId, flatId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Seeds the exact Flat B-902 bitemporal audit scenario:
     * Feb checkpoint (₹0), March maintenance + 2 cheques -> ₹2,000 on Mar 31,
     * followed in April by 2 bounced cheques (+₹6,500) and 4 backdated credit notes (-₹2,000),
     * plus April invoice (+₹8,500).
     */
    @Transactional
    public TimeTravelReconstructionDto seedFlatB902Scenario(long societyId) {
        long flatId = 902L;
        String unitCode = "Tower B - 902";

        if (eventRepository.findBySocietyIdAndFlatIdOrderByStreamVersionAsc(societyId, flatId).isEmpty()) {
            appendEvent(new AppendLedgerEventRequest(
                    societyId, flatId, unitCode, LedgerEventType.MAINTENANCE_INVOICE,
                    LocalDate.of(2026, 2, 1), Instant.parse("2026-02-01T03:30:00Z"),
                    500_000L, null, "FEB_2026_MAINTENANCE", "BILLING_CRON",
                    "b902-feb-inv", "{\"debit\":\"Receivable:B-902\",\"credit\":\"MaintenanceIncome\",\"paise\":500000}"
            ));
            appendEvent(new AppendLedgerEventRequest(
                    societyId, flatId, unitCode, LedgerEventType.PAYMENT_RECEIVED,
                    LocalDate.of(2026, 2, 10), Instant.parse("2026-02-10T06:30:00Z"),
                    -500_000L, null, "UPI_FEB_SETTLED", "RESIDENT_APP",
                    "b902-feb-pay", "{\"debit\":\"Bank:HDFC\",\"credit\":\"Receivable:B-902\",\"paise\":500000}"
            ));
            createCheckpoint(societyId, flatId, LocalDate.of(2026, 2, 28), Instant.parse("2026-02-28T18:29:59Z"));

            appendEvent(new AppendLedgerEventRequest(
                    societyId, flatId, unitCode, LedgerEventType.MAINTENANCE_INVOICE,
                    LocalDate.of(2026, 3, 1), Instant.parse("2026-03-01T03:30:00Z"),
                    850_000L, null, "MAR_2026_MAINTENANCE", "BILLING_CRON",
                    "b902-mar-inv", "{\"debit\":\"Receivable:B-902\",\"credit\":\"MaintenanceIncome\",\"paise\":850000}"
            ));
            BitemporalLedgerEventDto chq1 = appendEvent(new AppendLedgerEventRequest(
                    societyId, flatId, unitCode, LedgerEventType.CHEQUE_CLEARED,
                    LocalDate.of(2026, 3, 18), Instant.parse("2026-03-18T05:30:00Z"),
                    -400_000L, null, "CHQ_1041_PROVISIONAL", "ACCOUNTANT_MEERA",
                    "b902-mar-chq1", "{\"chequeNo\":\"1041\",\"paise\":400000}"
            ));
            BitemporalLedgerEventDto chq2 = appendEvent(new AppendLedgerEventRequest(
                    societyId, flatId, unitCode, LedgerEventType.CHEQUE_CLEARED,
                    LocalDate.of(2026, 3, 25), Instant.parse("2026-03-25T09:30:00Z"),
                    -250_000L, null, "CHQ_1042_PROVISIONAL", "ACCOUNTANT_MEERA",
                    "b902-mar-chq2", "{\"chequeNo\":\"1042\",\"paise\":250000}"
            ));

            // April retroactive reversals & backdated credit notes
            appendEvent(new AppendLedgerEventRequest(
                    societyId, flatId, unitCode, LedgerEventType.CHEQUE_BOUNCED_NSF,
                    LocalDate.of(2026, 3, 31), Instant.parse("2026-04-03T06:15:00Z"),
                    400_000L, chq1.eventId(), "NSF_BOUNCE_CHQ_1041", "BANK_WEBHOOK",
                    "b902-apr-nsf1", "{\"reversesCheque\":\"1041\",\"reason\":\"Insufficient Funds\"}"
            ));
            appendEvent(new AppendLedgerEventRequest(
                    societyId, flatId, unitCode, LedgerEventType.CHEQUE_BOUNCED_NSF,
                    LocalDate.of(2026, 3, 31), Instant.parse("2026-04-04T07:45:00Z"),
                    250_000L, chq2.eventId(), "NSF_BOUNCE_CHQ_1042", "BANK_WEBHOOK",
                    "b902-apr-nsf2", "{\"reversesCheque\":\"1042\",\"reason\":\"Signature Mismatch\"}"
            ));
            appendEvent(new AppendLedgerEventRequest(
                    societyId, flatId, unitCode, LedgerEventType.CREDIT_NOTE_BACKDATED,
                    LocalDate.of(2026, 3, 31), Instant.parse("2026-04-05T08:00:00Z"),
                    -50_000L, null, "CN_01_WATER_METER_ADJ", "TREASURER_RAO",
                    "b902-apr-cn1", "{\"creditNote\":\"CN-01\",\"paise\":50000}"
            ));
            appendEvent(new AppendLedgerEventRequest(
                    societyId, flatId, unitCode, LedgerEventType.CREDIT_NOTE_BACKDATED,
                    LocalDate.of(2026, 3, 31), Instant.parse("2026-04-05T08:10:00Z"),
                    -50_000L, null, "CN_02_SOLAR_REBATE", "TREASURER_RAO",
                    "b902-apr-cn2", "{\"creditNote\":\"CN-02\",\"paise\":50000}"
            ));
            appendEvent(new AppendLedgerEventRequest(
                    societyId, flatId, unitCode, LedgerEventType.CREDIT_NOTE_BACKDATED,
                    LocalDate.of(2026, 3, 31), Instant.parse("2026-04-06T09:20:00Z"),
                    -50_000L, null, "CN_03_AMENITY_REFUND", "TREASURER_RAO",
                    "b902-apr-cn3", "{\"creditNote\":\"CN-03\",\"paise\":50000}"
            ));
            appendEvent(new AppendLedgerEventRequest(
                    societyId, flatId, unitCode, LedgerEventType.CREDIT_NOTE_BACKDATED,
                    LocalDate.of(2026, 3, 31), Instant.parse("2026-04-07T10:00:00Z"),
                    -50_000L, null, "CN_04_GST_ROUNDING", "TREASURER_RAO",
                    "b902-apr-cn4", "{\"creditNote\":\"CN-04\",\"paise\":50000}"
            ));
            appendEvent(new AppendLedgerEventRequest(
                    societyId, flatId, unitCode, LedgerEventType.MAINTENANCE_INVOICE,
                    LocalDate.of(2026, 4, 1), Instant.parse("2026-04-01T03:30:00Z"),
                    850_000L, null, "APR_2026_MAINTENANCE", "BILLING_CRON",
                    "b902-apr-inv", "{\"debit\":\"Receivable:B-902\",\"credit\":\"MaintenanceIncome\",\"paise\":850000}"
            ));
        }

        return reconstructAtPointInTime(new TimeTravelQueryRequest(
                societyId,
                flatId,
                LocalDate.of(2026, 3, 31),
                Instant.parse("2026-03-31T18:29:59Z")
        ));
    }

    private BitemporalLedgerEventDto toDto(BitemporalLedgerEventEntity e) {
        return new BitemporalLedgerEventDto(
                e.getEventId(),
                e.getSocietyId(),
                e.getFlatId(),
                e.getUnitCode(),
                e.getStreamVersion(),
                e.getGlobalSeq(),
                e.getEventType(),
                e.getEffectiveDate(),
                e.getRecordedAt(),
                e.getSignedAmountPaise(),
                e.getCausesEventId(),
                e.getReasonCode(),
                e.getActorName(),
                e.getIdempotencyKey(),
                e.getLinesJson(),
                e.getPrevEventHash(),
                e.getEventHash()
        );
    }

    private LedgerCheckpointDto toCheckpointDto(LedgerCheckpointEntity cp) {
        return new LedgerCheckpointDto(
                cp.getCheckpointId(),
                cp.getSocietyId(),
                cp.getFlatId(),
                cp.getUnitCode(),
                cp.getCheckpointDate(),
                cp.getRecordedCutoff(),
                cp.getStreamVersion(),
                cp.getBalancePaise(),
                cp.getStateHash(),
                cp.getCreatedAt()
        );
    }
}
