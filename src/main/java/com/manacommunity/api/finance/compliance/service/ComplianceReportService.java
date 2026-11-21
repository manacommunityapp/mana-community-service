package com.manacommunity.api.finance.compliance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.finance.compliance.dto.GenerateReportRequest;
import com.manacommunity.api.finance.compliance.dto.ReportData;
import com.manacommunity.api.finance.compliance.dto.UpsertConfigRequest;
import com.manacommunity.api.finance.compliance.entity.ComplianceReportConfig;
import com.manacommunity.api.finance.compliance.entity.GeneratedReport;
import com.manacommunity.api.finance.compliance.repository.ComplianceReportConfigRepository;
import com.manacommunity.api.finance.compliance.repository.GeneratedReportRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplianceReportService {

    private final ComplianceReportConfigRepository configRepo;
    private final GeneratedReportRepository reportRepo;
    private final EntityManager em;
    private final ObjectMapper objectMapper;

    // ── AGM Financial Summary ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReportData generateAgmFinancialSummary(Long communityId, String period) {
        // Aggregate FINANCIAL audit entries for the period
        List<?> rows = em.createNativeQuery("""
            SELECT entity_type, action, COUNT(*) AS count,
                   COALESCE(SUM((payload->>'amount')::numeric), 0) AS total
            FROM audit_journal
            WHERE community_id = :cid
              AND category = 'FINANCIAL'
              AND created_at >= :start AND created_at < :end
            GROUP BY entity_type, action
            ORDER BY entity_type, action
        """)
        .setParameter("cid", communityId)
        .setParameter("start", periodStart(period))
        .setParameter("end", periodEnd(period))
        .getResultList();

        List<Map<String, Object>> rowMaps = toMaps(rows, "entity_type", "action", "count", "total");
        BigDecimal totalAmount = rowMaps.stream()
                .map(r -> toBd(r.get("total")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReportData(
                "AGM Financial Summary", period, rowMaps,
                Map.of("communityId", communityId, "period", period, "totalAmount", totalAmount),
                totalAmount, rowMaps.size());
    }

    // ── GST Summary ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReportData generateGstSummary(Long communityId, String monthYear) {
        // Maintenance charges > ₹7500/month per unit attract 18% GST
        List<?> rows = em.createNativeQuery("""
            SELECT fi.customer_name, fi.doc_date, fi.total_amount,
                   CASE WHEN fi.total_amount > 7500 THEN fi.total_amount * 0.18 ELSE 0 END AS gst_amount
            FROM finance_invoice fi
            WHERE fi.created_at >= :start AND fi.created_at < :end
              AND fi.total_amount > 7500
            ORDER BY fi.doc_date
        """)
        .setParameter("start", monthStart(monthYear))
        .setParameter("end", monthEnd(monthYear))
        .getResultList();

        List<Map<String, Object>> rowMaps = toMaps(rows, "customer_name", "doc_date", "total_amount", "gst_amount");
        BigDecimal gstTotal = rowMaps.stream()
                .map(r -> toBd(r.get("gst_amount")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReportData(
                "GST Summary (18% on maintenance >₹7500)", monthYear, rowMaps,
                Map.of("gstTotal", gstTotal, "taxableUnits", rowMaps.size()),
                gstTotal, rowMaps.size());
    }

    // ── TDS Summary ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReportData generateTdsSummary(Long communityId, String quarterYear) {
        // 194C: TDS on contractor payments > ₹30k per contract
        List<?> rows = em.createNativeQuery("""
            SELECT tc.vendor_name, tc.challan_date, tc.payment_amount,
                   tc.tds_amount, tc.challan_number
            FROM tds_challan tc
            WHERE tc.created_at >= :start AND tc.created_at < :end
              AND tc.payment_amount > 30000
            ORDER BY tc.challan_date
        """)
        .setParameter("start", quarterStart(quarterYear))
        .setParameter("end", quarterEnd(quarterYear))
        .getResultList();

        List<Map<String, Object>> rowMaps = toMaps(rows,
                "vendor_name", "challan_date", "payment_amount", "tds_amount", "challan_number");
        BigDecimal tdsTotal = rowMaps.stream()
                .map(r -> toBd(r.get("tds_amount")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ReportData(
                "TDS Summary (194C >₹30k)", quarterYear, rowMaps,
                Map.of("tdsTotal", tdsTotal, "entries", rowMaps.size()),
                tdsTotal, rowMaps.size());
    }

    // ── Maintenance Fund ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReportData generateMaintenanceFundReport(Long communityId, String period) {
        List<?> collections = em.createNativeQuery("""
            SELECT COALESCE(SUM((payload->>'amount')::numeric), 0)
            FROM audit_journal
            WHERE community_id = :cid AND category = 'FINANCIAL'
              AND action LIKE 'COLLECTION%'
              AND created_at >= :start AND created_at < :end
        """)
        .setParameter("cid", communityId)
        .setParameter("start", periodStart(period))
        .setParameter("end", periodEnd(period))
        .getResultList();

        List<?> expenses = em.createNativeQuery("""
            SELECT COALESCE(SUM((payload->>'amount')::numeric), 0)
            FROM audit_journal
            WHERE community_id = :cid AND category = 'FINANCIAL'
              AND action LIKE 'EXPENSE%'
              AND created_at >= :start AND created_at < :end
        """)
        .setParameter("cid", communityId)
        .setParameter("start", periodStart(period))
        .setParameter("end", periodEnd(period))
        .getResultList();

        BigDecimal totalCollections = toBd(collections.isEmpty() ? BigDecimal.ZERO : collections.get(0));
        BigDecimal totalExpenses = toBd(expenses.isEmpty() ? BigDecimal.ZERO : expenses.get(0));
        BigDecimal closingBalance = totalCollections.subtract(totalExpenses);

        List<Map<String, Object>> rows = List.of(
                Map.of("line", "Total Collections", "amount", totalCollections),
                Map.of("line", "Total Expenses", "amount", totalExpenses),
                Map.of("line", "Closing Balance", "amount", closingBalance));

        return new ReportData(
                "Maintenance Fund Report", period, rows,
                Map.of("collections", totalCollections, "expenses", totalExpenses, "balance", closingBalance),
                closingBalance, 3);
    }

    // ── Audit Export ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReportData generateAuditExport(Long communityId, String fromDate, String toDate) {
        List<?> rows = em.createNativeQuery("""
            SELECT audit_id, category, action, entity_type, entity_id,
                   actor_id, actor_type, created_at
            FROM audit_journal
            WHERE community_id = :cid
              AND created_at >= CAST(:from AS timestamptz)
              AND created_at < CAST(:to AS timestamptz)
            ORDER BY created_at
        """)
        .setParameter("cid", communityId)
        .setParameter("from", fromDate)
        .setParameter("to", toDate)
        .getResultList();

        List<Map<String, Object>> rowMaps = toMaps(rows,
                "audit_id", "category", "action", "entity_type", "entity_id",
                "actor_id", "actor_type", "created_at");

        return new ReportData(
                "Full Audit Export", fromDate + " to " + toDate, rowMaps,
                Map.of("communityId", communityId, "entries", rowMaps.size()),
                null, rowMaps.size());
    }

    // ── Scheduled monthly generation ──────────────────────────────────────────

    @Scheduled(cron = "0 2 1 * *")
    public void generateScheduledReports() {
        String period = YearMonth.now().minusMonths(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM"));
        log.info("Running scheduled compliance reports for period={}", period);

        List<ComplianceReportConfig> configs = configRepo.findByIsActiveTrue();
        for (ComplianceReportConfig cfg : configs) {
            try {
                ReportData data = switch (cfg.getReportType()) {
                    case "AGM_FINANCIAL"   -> generateAgmFinancialSummary(cfg.getCommunityId(), period);
                    case "GST_SUMMARY"     -> generateGstSummary(cfg.getCommunityId(), period);
                    case "TDS_SUMMARY"     -> generateTdsSummary(cfg.getCommunityId(), period + "-Q");
                    case "MAINTENANCE_FUND"-> generateMaintenanceFundReport(cfg.getCommunityId(), period);
                    default -> null;
                };
                if (data != null) storeReport(cfg.getCommunityId(), cfg.getReportType(), period, data, null);
            } catch (Exception e) {
                log.error("Scheduled report failed communityId={} type={}", cfg.getCommunityId(), cfg.getReportType(), e);
            }
        }
    }

    // ── On-demand generation ──────────────────────────────────────────────────

    @Transactional
    public GeneratedReport generateOnDemand(GenerateReportRequest req, Long requestedBy) {
        ReportData data = switch (req.getReportType()) {
            case "AGM_FINANCIAL"    -> generateAgmFinancialSummary(req.getCommunityId(), req.getPeriod());
            case "GST_SUMMARY"      -> generateGstSummary(req.getCommunityId(), req.getPeriod());
            case "TDS_SUMMARY"      -> generateTdsSummary(req.getCommunityId(), req.getPeriod());
            case "MAINTENANCE_FUND" -> generateMaintenanceFundReport(req.getCommunityId(), req.getPeriod());
            case "AUDIT_EXPORT"     -> generateAuditExport(req.getCommunityId(), req.getFromDate(), req.getToDate());
            default -> throw new IllegalArgumentException("Unknown report type: " + req.getReportType());
        };
        return storeReport(req.getCommunityId(), req.getReportType(), req.getPeriod(), data, requestedBy);
    }

    // ── Config upsert ─────────────────────────────────────────────────────────

    @Transactional
    public ComplianceReportConfig upsertConfig(UpsertConfigRequest req) {
        ComplianceReportConfig cfg = configRepo
                .findByCommunityIdAndReportType(req.getCommunityId(), req.getReportType())
                .orElseGet(() -> ComplianceReportConfig.builder()
                        .communityId(req.getCommunityId())
                        .reportType(req.getReportType())
                        .build());
        cfg.setFrequency(req.getFrequency());
        cfg.setScheduleCron(req.getScheduleCron());
        cfg.setRecipients(req.getRecipients());
        cfg.setActive(req.isActive());
        return configRepo.save(cfg);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private GeneratedReport storeReport(Long communityId, String type, String period,
                                        ReportData data, Long generatedBy) {
        String meta = null;
        try { meta = objectMapper.writeValueAsString(data.summary()); } catch (Exception ignored) {}

        GeneratedReport report = GeneratedReport.builder()
                .communityId(communityId)
                .reportType(type)
                .reportPeriod(period != null ? period : "")
                .totalAmount(data.totalAmount())
                .rowCount(data.rowCount())
                .generatedBy(generatedBy)
                .metadata(meta)
                .build();
        return reportRepo.save(report);
    }

    private Instant periodStart(String period) {
        // period = "yyyy-MM"
        return YearMonth.parse(period).atDay(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
    }

    private Instant periodEnd(String period) {
        return YearMonth.parse(period).atEndOfMonth().plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
    }

    private Instant monthStart(String monthYear) { return periodStart(monthYear); }
    private Instant monthEnd(String monthYear) { return periodEnd(monthYear); }

    private Instant quarterStart(String qy) {
        // qy = "2025-Q3"
        int year = Integer.parseInt(qy.substring(0, 4));
        int q = Integer.parseInt(qy.substring(6));
        int month = (q - 1) * 3 + 1;
        return YearMonth.of(year, month).atDay(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
    }

    private Instant quarterEnd(String qy) {
        int year = Integer.parseInt(qy.substring(0, 4));
        int q = Integer.parseInt(qy.substring(6));
        int month = q * 3;
        return YearMonth.of(year, month).atEndOfMonth().plusDays(1).atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
    }

    private BigDecimal toBd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toMaps(List<?> rows, String... cols) {
        return rows.stream().map(row -> {
            Object[] arr = row instanceof Object[] ? (Object[]) row : new Object[]{row};
            java.util.LinkedHashMap<String, Object> map = new java.util.LinkedHashMap<>();
            for (int i = 0; i < cols.length && i < arr.length; i++) map.put(cols[i], arr[i]);
            return (Map<String, Object>) map;
        }).toList();
    }
}
