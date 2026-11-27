package com.manacommunity.api.accounting.service;

import com.manacommunity.api.accounting.dto.AccountingDtos.*;
import com.manacommunity.api.accounting.entity.*;
import com.manacommunity.api.accounting.repository.*;
import com.manacommunity.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Bi-temporal, versioned Indian GST & TDS Rule Engine for RWAs / Cooperative Housing Societies.
 */
@Service
@RequiredArgsConstructor
public class StatutoryTaxRuleService {

    public static final String FAMILY_GST_MAINTENANCE = "GST_MAINTENANCE";
    public static final String FAMILY_TDS_194C = "TDS_194C";
    public static final String FAMILY_TDS_194J = "TDS_194J";
    public static final String FAMILY_TDS_194I = "TDS_194I";

    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]$");
    private static final long DEFAULT_TURNOVER_THRESHOLD_PAISE = 200_000_000L; // ₹20 lakh
    private static final long DEFAULT_MAINT_CLIFF_PAISE = 750_000L;         // ₹7,500 / flat / month
    private static final int DEFAULT_GST_RATE_BPS = 1800;                   // 18%

    private final TaxRuleVersionRepository ruleRepository;
    private final VendorPanRegistryRepository panRegistryRepository;
    private final VendorFyTdsLedgerRepository tdsLedgerRepository;
    private final TaxCalculationSnapshotRepository snapshotRepository;
    private final TaxRecalculationBatchRepository batchRepository;
    private final TaxAdjustmentNoteRepository noteRepository;
    private final ImmutableAuditChainService auditChainService;

    @Transactional
    public TaxRuleVersionDto publishRuleVersion(PublishTaxRuleRequest req) {
        LocalDate effectiveFrom = LocalDate.parse(req.effectiveFrom());
        TaxRuleVersionEntity latest = ruleRepository
                .findTopByRuleFamilyAndFyCodeOrderByVersionNoDesc(req.ruleFamily(), req.fyCode())
                .orElse(null);

        int nextVersion = 1;
        if (latest != null) {
            nextVersion = latest.getVersionNo() + 1;
            if (latest.getEffectiveTo() == null || !latest.getEffectiveTo().isBefore(effectiveFrom)) {
                latest.setEffectiveTo(effectiveFrom.minusDays(1));
                ruleRepository.save(latest);
            }
        }

        Map<String, Object> spec = req.ruleSpec() != null ? req.ruleSpec() : Map.of();
        TaxRuleVersionEntity entity = new TaxRuleVersionEntity();
        entity.setRuleVersionId(UUID.randomUUID());
        entity.setRuleFamily(req.ruleFamily());
        entity.setFyCode(req.fyCode());
        entity.setVersionNo(nextVersion);
        entity.setEffectiveFrom(effectiveFrom);
        entity.setEffectiveTo(null);
        entity.setGazetteRef(req.gazetteRef());
        entity.setRuleSpec(spec);
        entity.setSpecSha256(auditChainService.sha256Hex(auditChainService.canonicalJson(spec)));
        ruleRepository.save(entity);

        return toRuleDto(entity);
    }

    @Transactional(readOnly = true)
    public List<TaxRuleVersionDto> listRuleVersions(String ruleFamily, String fyCode) {
        List<TaxRuleVersionEntity> list = (ruleFamily != null && !ruleFamily.isBlank() && fyCode != null && !fyCode.isBlank())
                ? ruleRepository.findByRuleFamilyAndFyCodeOrderByVersionNoDesc(ruleFamily, fyCode)
                : ruleRepository.findAllByOrderByFyCodeDescVersionNoDesc();
        return list.stream().map(this::toRuleDto).toList();
    }

    @Transactional
    public MaintenanceGstResult calculateMaintenanceGst(MaintenanceGstRequest req) {
        LocalDate supplyDate = LocalDate.parse(req.supplyDate());
        TaxRuleVersionEntity rule = resolveRule(FAMILY_GST_MAINTENANCE, req.fyCode(), supplyDate);
        MaintenanceGstResult result = evaluateMaintenanceGst(req, rule);

        if (req.persistSnapshot()) {
            UUID voucherUuid = UUID.randomUUID();
            TaxCalculationSnapshotEntity snap = new TaxCalculationSnapshotEntity();
            snap.setSnapshotId(UUID.randomUUID());
            snap.setSocietyId(req.societyId());
            snap.setVoucherId(voucherUuid);
            snap.setVoucherNumber(req.voucherNumber() != null ? req.voucherNumber() : "MNT-PREVIEW");
            snap.setRuleVersionId(UUID.fromString(result.ruleVersionId()));
            snap.setRuleFamily(FAMILY_GST_MAINTENANCE);
            snap.setFyCode(req.fyCode());
            snap.setSupplyDate(supplyDate);
            snap.setBaseAmountPaise(result.maintenanceBasePaise());
            snap.setComputedTaxPaise(result.totalGstPaise());
            snap.setCgstPaise(result.cgstPaise());
            snap.setSgstPaise(result.sgstPaise());
            snap.setAppliedRateBps(result.appliedRateBps());
            snap.setInputsJson(Map.of(
                    "societyGstRegistered", req.societyGstRegistered(),
                    "societyTurnoverPaise", req.societyTurnoverPaise(),
                    "flatNumber", req.flatNumber() != null ? req.flatNumber() : "",
                    "maintenanceBasePaise", result.maintenanceBasePaise(),
                    "pureAgentExcludedPaise", result.pureAgentExcludedPaise()
            ));
            snap.setSnapshotSha256(result.snapshotSha256());
            snapshotRepository.save(snap);
        }

        return result;
    }

    public MaintenanceGstResult evaluateMaintenanceGst(MaintenanceGstRequest req, TaxRuleVersionEntity rule) {
        Map<String, Object> spec = rule != null && rule.getRuleSpec() != null ? rule.getRuleSpec() : Map.of();
        long turnoverThreshold = longVal(spec, "turnoverThresholdPaise", DEFAULT_TURNOVER_THRESHOLD_PAISE);
        long cliffThreshold = longVal(spec, "perUnitMonthlyThresholdPaise", DEFAULT_MAINT_CLIFF_PAISE);
        int gstRateBps = intVal(spec, "gstRateBps", DEFAULT_GST_RATE_BPS);

        long maintBase = 0L;
        long pureAgentExcluded = 0L;
        if (req.lines() != null) {
            for (MaintenanceChargeLine line : req.lines()) {
                if (line.pureAgentPassThrough()) {
                    pureAgentExcluded += line.amountPaise();
                } else {
                    maintBase += line.amountPaise();
                }
            }
        }
        long totalBilled = maintBase + pureAgentExcluded;
        boolean registered = req.societyGstRegistered() || req.societyTurnoverPaise() > turnoverThreshold;

        boolean gstApplicable = false;
        String reason;
        long cgst = 0L;
        long sgst = 0L;
        int appliedBps = 0;

        if (!registered) {
            reason = "Exempt: society turnover within ₹20L threshold and unregistered";
        } else if (maintBase <= cliffThreshold) {
            reason = "Exempt under Notification 12/2017 Entry 77 (<= ₹7,500/flat/month)";
        } else {
            gstApplicable = true;
            appliedBps = gstRateBps;
            long totalTax = Math.round((maintBase * (double) gstRateBps) / 10_000.0);
            cgst = totalTax / 2L;
            sgst = totalTax - cgst;
            reason = "Taxable cliff triggered (> ₹7,500/flat/month): 18% GST applied on ENTIRE ₹"
                    + (maintBase / 100.0);
        }

        long totalGst = cgst + sgst;
        String snapshotSha = auditChainService.sha256Hex(
                (rule != null ? rule.getRuleVersionId().toString() : "DEFAULT")
                        + "|" + maintBase + "|" + totalGst + "|" + appliedBps
        );

        return new MaintenanceGstResult(
                rule != null ? rule.getRuleVersionId().toString() : UUID.nameUUIDFromBytes("DEFAULT_GST".getBytes()).toString(),
                req.fyCode(),
                req.supplyDate(),
                totalBilled,
                maintBase,
                pureAgentExcluded,
                gstApplicable,
                reason,
                appliedBps,
                cgst,
                sgst,
                totalGst,
                totalBilled + totalGst,
                snapshotSha
        );
    }

    public PanValidationResult validatePan(String rawPan, String statusOverride) {
        String pan = rawPan != null ? rawPan.trim().toUpperCase(Locale.ROOT) : "";
        boolean formatValid = PAN_PATTERN.matcher(pan).matches();
        if (!formatValid) {
            return new PanValidationResult(pan, false, "-", "INVALID_OR_MISSING", "INVALID", true);
        }

        String entityChar = String.valueOf(pan.charAt(3));
        String category = switch (entityChar) {
            case "P" -> "INDIVIDUAL";
            case "H" -> "HUF";
            case "C" -> "COMPANY";
            case "F" -> "FIRM";
            case "A" -> "AOP";
            case "T" -> "TRUST";
            case "B" -> "BOI";
            default -> "OTHER";
        };

        String status = statusOverride != null && !statusOverride.isBlank()
                ? statusOverride.toUpperCase(Locale.ROOT)
                : panRegistryRepository.findById(pan).map(VendorPanRegistryEntity::getPanStatus).orElse("VALID");

        boolean penal = !"VALID".equals(status);
        return new PanValidationResult(pan, true, entityChar, category, status, penal);
    }

    @Transactional
    public VendorTdsResult calculateVendorTds(VendorTdsRequest req) {
        LocalDate txDate = LocalDate.parse(req.creditOrPaymentDate());
        String section = req.tdsSection() != null ? req.tdsSection().toUpperCase(Locale.ROOT) : "194C";
        String family = section.startsWith("194J") ? FAMILY_TDS_194J
                : section.startsWith("194I") ? FAMILY_TDS_194I
                : FAMILY_TDS_194C;

        TaxRuleVersionEntity rule = resolveRule(family, req.fyCode(), txDate);
        PanValidationResult panVal = validatePan(req.vendorPan(), req.panStatusOverride());

        int rateBps;
        long singleThresholdPaise = 0L;
        long aggregateThresholdPaise;

        if (panVal.penal206aaTriggered()) {
            rateBps = 2000; // Sec 206AA: 20%
            aggregateThresholdPaise = 0L;
        } else if ("194C".equals(section)) {
            rateBps = ("P".equals(panVal.entityTypeChar()) || "H".equals(panVal.entityTypeChar())) ? 100 : 200;
            singleThresholdPaise = 3_000_000L;    // ₹30,000
            aggregateThresholdPaise = 10_000_000L; // ₹1,00,000
        } else if ("194J_TECH".equals(section)) {
            rateBps = 200;
            aggregateThresholdPaise = 3_000_000L; // ₹30,000
        } else if ("194J_PROF".equals(section) || "194J".equals(section)) {
            rateBps = 1000;
            aggregateThresholdPaise = 3_000_000L;
        } else if ("194I_EQUIP".equals(section)) {
            rateBps = 200;
            aggregateThresholdPaise = 24_000_000L; // ₹2,40,000
        } else {
            rateBps = 1000; // 194I_LAND
            aggregateThresholdPaise = 24_000_000L;
        }

        VendorFyTdsLedgerEntity ledger = tdsLedgerRepository
                .findForUpdate(req.societyId(), req.fyCode(), panVal.pan(), section)
                .orElseGet(() -> {
                    VendorFyTdsLedgerEntity created = new VendorFyTdsLedgerEntity();
                    created.setSocietyId(req.societyId());
                    created.setFyCode(req.fyCode());
                    created.setVendorPan(panVal.pan());
                    created.setTdsSection(section);
                    return created;
                });

        long priorGross = ledger.getCumulativeGrossPaise();
        long priorTds = ledger.getCumulativeTdsPaise();
        long newGross = priorGross + req.grossAmountPaise();

        boolean thresholdCrossed = panVal.penal206aaTriggered()
                || (singleThresholdPaise > 0 && req.grossAmountPaise() > singleThresholdPaise)
                || newGross > aggregateThresholdPaise;

        long tdsNow;
        boolean catchUp = false;
        String reason;

        if (!thresholdCrossed) {
            tdsNow = 0L;
            reason = "Below single & aggregate FY TDS threshold";
        } else {
            long requiredCumulativeTds = Math.round((newGross * (double) rateBps) / 10_000.0);
            long incrementalTds = Math.round((req.grossAmountPaise() * (double) rateBps) / 10_000.0);
            if (priorTds == 0L && priorGross > 0L && newGross > aggregateThresholdPaise) {
                tdsNow = Math.max(0L, requiredCumulativeTds - priorTds);
                catchUp = true;
                reason = "Aggregate FY threshold crossed: catch-up TDS deducted on cumulative ₹" + (newGross / 100.0);
            } else {
                tdsNow = incrementalTds;
                reason = panVal.penal206aaTriggered()
                        ? "Sec 206AA penal 20% TDS applied (missing/inoperative PAN)"
                        : "Standard " + section + " TDS @ " + (rateBps / 100.0) + "%";
            }
        }

        if (req.persistSnapshot()) {
            ledger.setCumulativeGrossPaise(newGross);
            ledger.setCumulativeTdsPaise(priorTds + tdsNow);
            tdsLedgerRepository.save(ledger);
        }

        return new VendorTdsResult(
                rule != null ? rule.getRuleVersionId().toString() : UUID.nameUUIDFromBytes(family.getBytes()).toString(),
                section,
                panVal,
                req.grossAmountPaise(),
                priorGross,
                newGross,
                thresholdCrossed,
                catchUp,
                rateBps,
                tdsNow,
                Math.max(0L, req.grossAmountPaise() - tdsNow),
                reason
        );
    }

    /**
     * Re-evaluates historical snapshots in [fromDate, toDate] against a new rule version
     * and emits GST Credit/Debit Notes without mutating original posted snapshots.
     */
    @Transactional
    public RecalculationBatchDto recalculateRange(RecalculateTaxRequest req) {
        UUID targetRuleId = UUID.fromString(req.targetRuleVersionId());
        TaxRuleVersionEntity targetRule = ruleRepository.findById(targetRuleId)
                .orElseThrow(() -> new ResourceNotFoundException("TaxRuleVersion", "ruleVersionId", req.targetRuleVersionId()));

        LocalDate from = LocalDate.parse(req.fromDate());
        LocalDate to = LocalDate.parse(req.toDate());
        List<TaxCalculationSnapshotEntity> snapshots = snapshotRepository
                .findBySocietyIdAndFyCodeAndRuleFamilyAndSupplyDateBetweenOrderBySupplyDateAsc(
                        req.societyId(), req.fyCode(), req.ruleFamily(), from, to);

        UUID batchId = UUID.randomUUID();
        TaxRecalculationBatchEntity batch = new TaxRecalculationBatchEntity();
        batch.setBatchId(batchId);
        batch.setSocietyId(req.societyId());
        batch.setFyCode(req.fyCode());
        batch.setRuleFamily(req.ruleFamily());
        batch.setTargetRuleVersionId(targetRuleId);
        batch.setFromDate(from);
        batch.setToDate(to);
        batch.setEvaluatedCount(snapshots.size());
        batchRepository.save(batch);

        List<TaxAdjustmentNoteDto> notes = new ArrayList<>();
        int adjusted = 0;
        long netDelta = 0L;
        int seq = 1;

        for (TaxCalculationSnapshotEntity snap : snapshots) {
            if (snap.getRuleVersionId().equals(targetRuleId)) {
                continue;
            }
            MaintenanceGstRequest replayReq = new MaintenanceGstRequest(
                    snap.getSocietyId(),
                    snap.getFyCode(),
                    snap.getSupplyDate().toString(),
                    String.valueOf(snap.getInputsJson().getOrDefault("flatNumber", "")),
                    snap.getVoucherNumber(),
                    Boolean.TRUE.equals(snap.getInputsJson().get("societyGstRegistered")),
                    ((Number) snap.getInputsJson().getOrDefault("societyTurnoverPaise", 300_000_000L)).longValue(),
                    List.of(new MaintenanceChargeLine("MAINT", "Maintenance", snap.getBaseAmountPaise(), false)),
                    false
            );
            MaintenanceGstResult recomputed = evaluateMaintenanceGst(replayReq, targetRule);
            long delta = recomputed.totalGstPaise() - snap.getComputedTaxPaise();
            if (delta == 0L) {
                continue;
            }

            String noteType = delta > 0 ? "DEBIT_NOTE" : "CREDIT_NOTE";
            String noteNum = (delta > 0 ? "DN/" : "CN/") + req.fyCode() + "/" + String.format("%05d", seq++);

            TaxAdjustmentNoteEntity note = new TaxAdjustmentNoteEntity();
            note.setNoteId(UUID.randomUUID());
            note.setBatchId(batchId);
            note.setSocietyId(req.societyId());
            note.setOriginalVoucherId(snap.getVoucherId());
            note.setNoteType(noteType);
            note.setNoteNumber(noteNum);
            note.setDeltaPaise(delta);
            note.setRuleVersionId(targetRuleId);
            noteRepository.save(note);

            notes.add(new TaxAdjustmentNoteDto(
                    note.getNoteId().toString(),
                    snap.getVoucherId().toString(),
                    noteType,
                    noteNum,
                    delta,
                    targetRuleId.toString()
            ));
            adjusted++;
            netDelta += delta;
        }

        batch.setAdjustedCount(adjusted);
        batch.setNetDeltaPaise(netDelta);
        batchRepository.save(batch);

        return new RecalculationBatchDto(
                batchId.toString(),
                req.societyId(),
                req.fyCode(),
                req.ruleFamily(),
                targetRuleId.toString(),
                snapshots.size(),
                adjusted,
                netDelta,
                notes
        );
    }

    private TaxRuleVersionEntity resolveRule(String family, String fyCode, LocalDate supplyDate) {
        List<TaxRuleVersionEntity> matches = ruleRepository.findMatchingRules(family, fyCode, supplyDate);
        return matches.isEmpty() ? null : matches.get(0);
    }

    private TaxRuleVersionDto toRuleDto(TaxRuleVersionEntity e) {
        return new TaxRuleVersionDto(
                e.getRuleVersionId().toString(),
                e.getRuleFamily(),
                e.getFyCode(),
                e.getVersionNo(),
                e.getEffectiveFrom().toString(),
                e.getEffectiveTo() != null ? e.getEffectiveTo().toString() : null,
                e.getGazetteRef(),
                e.getRuleSpec(),
                e.getSpecSha256()
        );
    }

    private long longVal(Map<String, Object> map, String key, long fallback) {
        Object val = map.get(key);
        return val instanceof Number n ? n.longValue() : fallback;
    }

    private int intVal(Map<String, Object> map, String key, int fallback) {
        Object val = map.get(key);
        return val instanceof Number n ? n.intValue() : fallback;
    }
}
