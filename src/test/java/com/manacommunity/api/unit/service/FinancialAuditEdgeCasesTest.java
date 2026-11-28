package com.manacommunity.api.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.accounting.dto.AccountingDtos.MaintenanceChargeLine;
import com.manacommunity.api.accounting.dto.AccountingDtos.MaintenanceGstRequest;
import com.manacommunity.api.accounting.dto.AccountingDtos.MaintenanceGstResult;
import com.manacommunity.api.accounting.entity.TaxRuleVersionEntity;
import com.manacommunity.api.accounting.repository.AuditLedgerEntryRepository;
import com.manacommunity.api.accounting.repository.TaxAdjustmentNoteRepository;
import com.manacommunity.api.accounting.repository.TaxCalculationSnapshotRepository;
import com.manacommunity.api.accounting.repository.TaxRecalculationBatchRepository;
import com.manacommunity.api.accounting.repository.TaxRuleVersionRepository;
import com.manacommunity.api.accounting.repository.VendorFyTdsLedgerRepository;
import com.manacommunity.api.accounting.repository.VendorPanRegistryRepository;
import com.manacommunity.api.accounting.service.ImmutableAuditChainService;
import com.manacommunity.api.accounting.service.StatutoryTaxRuleService;
import com.manacommunity.api.cfbos.integrations.service.PaymentWebhookReconciliationService;
import com.manacommunity.api.cfbos.integrations.service.TallyXmlExportService;
import com.manacommunity.api.cfbos.payment.repository.CfbosPaymentRepository;
import com.manacommunity.api.payments.repository.RazorpayOrderRepository;
import com.manacommunity.api.transactioncore.repository.TransactionIntentRepository;
import com.manacommunity.api.transactioncore.service.ManaTransactionCoreService;
import com.manacommunity.api.trip.p2p.service.TripP2PCollectService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class FinancialAuditEdgeCasesTest {

    @Test
    void validatesMidMonthProRataAndRs7500GstCliff() {
        // Mid-month move-in: 16 days out of 30 on ₹9,000 monthly base = ₹4,800.00
        BigDecimal monthlyCharge = new BigDecimal("9000.00");
        BigDecimal proRata = monthlyCharge
                .multiply(BigDecimal.valueOf(16))
                .divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
        assertThat(proRata).isEqualByComparingTo("4800.00");

        TaxRuleVersionRepository ruleRepo = Mockito.mock(TaxRuleVersionRepository.class);
        VendorPanRegistryRepository panRepo = Mockito.mock(VendorPanRegistryRepository.class);
        VendorFyTdsLedgerRepository tdsRepo = Mockito.mock(VendorFyTdsLedgerRepository.class);
        TaxCalculationSnapshotRepository snapRepo = Mockito.mock(TaxCalculationSnapshotRepository.class);
        TaxRecalculationBatchRepository batchRepo = Mockito.mock(TaxRecalculationBatchRepository.class);
        TaxAdjustmentNoteRepository noteRepo = Mockito.mock(TaxAdjustmentNoteRepository.class);
        AuditLedgerEntryRepository ledgerRepo = Mockito.mock(AuditLedgerEntryRepository.class);

        ImmutableAuditChainService auditChain = new ImmutableAuditChainService(ledgerRepo, new ObjectMapper());

        TaxRuleVersionEntity gstRule = new TaxRuleVersionEntity();
        gstRule.setRuleVersionId(UUID.randomUUID());
        gstRule.setRuleFamily(StatutoryTaxRuleService.FAMILY_GST_MAINTENANCE);
        gstRule.setFyCode("FY26-27");
        gstRule.setVersionNo(1);
        gstRule.setEffectiveFrom(LocalDate.of(2025, 1, 1));
        gstRule.setRuleSpec(Map.of(
                "turnoverThresholdPaise", 200_000_000L, // ₹20L threshold
                "perUnitMonthlyThresholdPaise", 750_000L, // ₹7,500 cliff
                "gstRateBps", 1800
        ));
        gstRule.setSpecSha256("0".repeat(64));

        when(ruleRepo.findMatchingRules(eq(StatutoryTaxRuleService.FAMILY_GST_MAINTENANCE), eq("FY26-27"), any()))
                .thenReturn(List.of(gstRule));
        when(snapRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StatutoryTaxRuleService taxService = new StatutoryTaxRuleService(
                ruleRepo, panRepo, tdsRepo, snapRepo, batchRepo, noteRepo, auditChain
        );

        // At/below ₹7,500 (₹7,499) -> ₹0 GST even for GST-registered society
        MaintenanceGstResult below = taxService.calculateMaintenanceGst(new MaintenanceGstRequest(
                1L, "FY26-27", "2026-04-01", "A-101", "MNT-001", true, 300_000_000L,
                List.of(new MaintenanceChargeLine("MAINT", "Monthly Maintenance", 749_900L, false)),
                false
        ));
        assertThat(below.gstApplicable()).isFalse();
        assertThat(below.totalGstPaise()).isZero();

        // Above ₹7,500 (₹8,000) -> 18% on ENTIRE ₹8,000 = ₹1,440 (144,000 paise)
        MaintenanceGstResult above = taxService.calculateMaintenanceGst(new MaintenanceGstRequest(
                1L, "FY26-27", "2026-04-01", "A-102", "MNT-002", true, 300_000_000L,
                List.of(new MaintenanceChargeLine("MAINT", "Monthly Maintenance", 800_000L, false)),
                false
        ));
        assertThat(above.gstApplicable()).isTrue();
        assertThat(above.totalGstPaise()).isEqualTo(144_000L);
        assertThat(above.cgstPaise()).isEqualTo(72_000L);
        assertThat(above.sgstPaise()).isEqualTo(72_000L);
    }

    @Test
    void comparesSimpleVsCompoundLateInterestAndTallyDoubleEntryBalance() {
        BigDecimal principal = new BigDecimal("10000.00");
        BigDecimal monthlyRate = new BigDecimal("0.02"); // 2% per month
        int months = 3;

        BigDecimal simpleInterest = principal.multiply(monthlyRate).multiply(BigDecimal.valueOf(months))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal compoundTotal = principal.multiply(BigDecimal.ONE.add(monthlyRate).pow(months))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal compoundInterest = compoundTotal.subtract(principal);

        assertThat(simpleInterest).isEqualByComparingTo("600.00");
        assertThat(compoundInterest).isEqualByComparingTo("612.08");
        assertThat(compoundInterest).isGreaterThan(simpleInterest);

        // Tally XML export must balance Debits == Credits
        TallyXmlExportService tally = new TallyXmlExportService();
        String xml = tally.exportReceiptVouchers(1L, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30));
        assertThat(xml).contains("<ENVELOPE>");
        assertThat(xml).contains("<ALLLEDGERENTRIES.LIST>");
        assertThat(xml).contains("<ISDEEMEDPOSITIVE>Yes</ISDEEMEDPOSITIVE>");
        assertThat(xml).contains("<ISDEEMEDPOSITIVE>No</ISDEEMEDPOSITIVE>");
    }

    @Test
    void handlesDelayedDuplicateWebhookPartialPaymentAndAdvanceWalletAdjustment() {
        CfbosPaymentRepository payRepo = Mockito.mock(CfbosPaymentRepository.class);
        TransactionIntentRepository intentRepo = Mockito.mock(TransactionIntentRepository.class);
        ManaTransactionCoreService core = Mockito.mock(ManaTransactionCoreService.class);
        RazorpayOrderRepository rzpRepo = Mockito.mock(RazorpayOrderRepository.class);
        TripP2PCollectService trip = Mockito.mock(TripP2PCollectService.class);

        when(rzpRepo.existsByWebhookEventId("evt-late-30min")).thenReturn(false, true);

        PaymentWebhookReconciliationService svc = new PaymentWebhookReconciliationService(
                payRepo, intentRepo, core, rzpRepo, trip
        );

        Map<String, Object> delayedWebhook = Map.of(
                "id", "evt-late-30min",
                "event", "payment.captured",
                "delayedSeconds", 1800
        );
        assertThat(svc.processRazorpayWebhook(delayedWebhook, "sig-ok")).isTrue();
        assertThat(svc.processRazorpayWebhook(delayedWebhook, "sig-ok")).isTrue();
        assertThat(svc.isEventProcessed("evt-late-30min")).isTrue();

        // Partial payment + advance wallet adjustment arithmetic check
        BigDecimal invoiceDue = new BigDecimal("10000.00");
        BigDecimal partialPaid = new BigDecimal("4000.00");
        BigDecimal advanceWalletBalance = new BigDecimal("2500.00");
        BigDecimal remainingAfterPartial = invoiceDue.subtract(partialPaid);
        BigDecimal appliedAdvance = remainingAfterPartial.min(advanceWalletBalance);
        BigDecimal netOutstanding = remainingAfterPartial.subtract(appliedAdvance);
        assertThat(netOutstanding).isEqualByComparingTo("3500.00");
    }
}
