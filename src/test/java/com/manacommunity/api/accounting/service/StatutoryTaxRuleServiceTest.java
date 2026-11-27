package com.manacommunity.api.accounting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.manacommunity.api.accounting.dto.AccountingDtos.*;
import com.manacommunity.api.accounting.entity.*;
import com.manacommunity.api.accounting.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class StatutoryTaxRuleServiceTest {

    private final TaxRuleVersionRepository ruleRepo = Mockito.mock(TaxRuleVersionRepository.class);
    private final VendorPanRegistryRepository panRepo = Mockito.mock(VendorPanRegistryRepository.class);
    private final VendorFyTdsLedgerRepository tdsLedgerRepo = Mockito.mock(VendorFyTdsLedgerRepository.class);
    private final TaxCalculationSnapshotRepository snapRepo = Mockito.mock(TaxCalculationSnapshotRepository.class);
    private final TaxRecalculationBatchRepository batchRepo = Mockito.mock(TaxRecalculationBatchRepository.class);
    private final TaxAdjustmentNoteRepository noteRepo = Mockito.mock(TaxAdjustmentNoteRepository.class);
    private final AuditLedgerEntryRepository ledgerRepo = Mockito.mock(AuditLedgerEntryRepository.class);
    private final ImmutableAuditChainService auditService = new ImmutableAuditChainService(ledgerRepo, new ObjectMapper());

    private final StatutoryTaxRuleService service = new StatutoryTaxRuleService(
            ruleRepo, panRepo, tdsLedgerRepo, snapRepo, batchRepo, noteRepo, auditService
    );

    @Test
    void applies18PctGstOnEntireAmountAbove7500CliffAndExcludesPureAgent() {
        when(ruleRepo.findMatchingRules(anyString(), anyString(), any())).thenReturn(List.of());

        // Case 1: ₹7,500 maintenance + ₹2,000 pure-agent electricity -> Exempt
        MaintenanceGstResult atThreshold = service.calculateMaintenanceGst(new MaintenanceGstRequest(
                1L, "2026-27", "2026-11-01", "A-401", "MNT-1",
                true, 300_000_000L,
                List.of(
                        new MaintenanceChargeLine("MAINT", "Monthly Maintenance", 750_000L, false),
                        new MaintenanceChargeLine("ELEC", "BESCOM Pass-through", 200_000L, true)
                ),
                false
        ));
        assertFalse(atThreshold.gstApplicable());
        assertEquals(0L, atThreshold.totalGstPaise());
        assertEquals(750_000L, atThreshold.maintenanceBasePaise());
        assertEquals(200_000L, atThreshold.pureAgentExcludedPaise());

        // Case 2: ₹7,501 maintenance -> 18% on ENTIRE ₹7,501 (135,018 paise)
        MaintenanceGstResult aboveCliff = service.calculateMaintenanceGst(new MaintenanceGstRequest(
                1L, "2026-27", "2026-11-01", "A-402", "MNT-2",
                true, 300_000_000L,
                List.of(new MaintenanceChargeLine("MAINT", "Monthly Maintenance", 750_100L, false)),
                false
        ));
        assertTrue(aboveCliff.gstApplicable());
        assertEquals(1800, aboveCliff.appliedRateBps());
        assertEquals(135_018L, aboveCliff.totalGstPaise());
        assertEquals(67_509L, aboveCliff.cgstPaise());
        assertEquals(67_509L, aboveCliff.sgstPaise());
    }

    @Test
    void validatesPanEntityTypes206aaAndCumulativeCatchUpTds() {
        when(ruleRepo.findMatchingRules(anyString(), anyString(), any())).thenReturn(List.of());
        when(panRepo.findById(anyString())).thenReturn(Optional.empty());

        // 194C Individual PAN (4th char P) -> 1% on ₹40,000 = ₹400
        when(tdsLedgerRepo.findForUpdate(anyLong(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        VendorTdsResult ind = service.calculateVendorTds(new VendorTdsRequest(
                1L, "2026-27", "2026-11-05", "PUR-1", "ABCDE1234F", "VALID", "194C", 4_000_000L, false
        ));
        // 4th char of ABCDE1234F is 'D' -> 2%, let's also test 'P' explicitly
        assertEquals(200, ind.appliedRateBps());

        VendorTdsResult actualInd = service.calculateVendorTds(new VendorTdsRequest(
                1L, "2026-27", "2026-11-05", "PUR-2", "ABCPD1234F", "VALID", "194C", 4_000_000L, false
        ));
        assertEquals("INDIVIDUAL", actualInd.panValidation().entityCategory());
        assertEquals(100, actualInd.appliedRateBps());
        assertEquals(40_000L, actualInd.tdsDeductedPaise());

        // Sec 206AA inoperative PAN -> 20%
        VendorTdsResult penal = service.calculateVendorTds(new VendorTdsRequest(
                1L, "2026-27", "2026-11-05", "PUR-3", "ABCPD1234F", "INOPERATIVE", "194C", 1_000_000L, false
        ));
        assertTrue(penal.panValidation().penal206aaTriggered());
        assertEquals(2000, penal.appliedRateBps());
        assertEquals(200_000L, penal.tdsDeductedPaise());

        // Cumulative catch-up: ₹84,000 prior untaxed + ₹25,000 bill -> ₹1,09,000 (> ₹1L) => 2% of ₹1,09,000 = ₹2,180
        VendorFyTdsLedgerEntity prior = new VendorFyTdsLedgerEntity();
        prior.setSocietyId(1L);
        prior.setFyCode("2026-27");
        prior.setVendorPan("ABCCD1234F");
        prior.setTdsSection("194C");
        prior.setCumulativeGrossPaise(8_400_000L);
        prior.setCumulativeTdsPaise(0L);
        when(tdsLedgerRepo.findForUpdate(1L, "2026-27", "ABCCD1234F", "194C")).thenReturn(Optional.of(prior));

        VendorTdsResult catchUp = service.calculateVendorTds(new VendorTdsRequest(
                1L, "2026-27", "2026-11-10", "PUR-4", "ABCCD1234F", "VALID", "194C", 2_500_000L, true
        ));
        assertTrue(catchUp.thresholdCrossed());
        assertTrue(catchUp.catchUpApplied());
        assertEquals(218_000L, catchUp.tdsDeductedPaise());
    }

    @Test
    void recalculatesMidFyRuleChangeIntoCreditNoteWithoutMutatingOriginal() {
        UUID newRuleId = UUID.randomUUID();
        TaxRuleVersionEntity raisedThresholdRule = new TaxRuleVersionEntity();
        raisedThresholdRule.setRuleVersionId(newRuleId);
        raisedThresholdRule.setRuleFamily(StatutoryTaxRuleService.FAMILY_GST_MAINTENANCE);
        raisedThresholdRule.setFyCode("2026-27");
        raisedThresholdRule.setVersionNo(2);
        raisedThresholdRule.setEffectiveFrom(LocalDate.of(2026, 4, 1));
        raisedThresholdRule.setRuleSpec(Map.of("perUnitMonthlyThresholdPaise", 1_000_000L, "gstRateBps", 1800));

        TaxCalculationSnapshotEntity oldSnap = new TaxCalculationSnapshotEntity();
        oldSnap.setSnapshotId(UUID.randomUUID());
        oldSnap.setSocietyId(1L);
        oldSnap.setVoucherId(UUID.randomUUID());
        oldSnap.setVoucherNumber("MNT/26-27/001");
        oldSnap.setRuleVersionId(UUID.randomUUID());
        oldSnap.setRuleFamily(StatutoryTaxRuleService.FAMILY_GST_MAINTENANCE);
        oldSnap.setFyCode("2026-27");
        oldSnap.setSupplyDate(LocalDate.of(2026, 5, 1));
        oldSnap.setBaseAmountPaise(850_000L);     // ₹8,500
        oldSnap.setComputedTaxPaise(153_000L);   // previously taxed ₹1,530 at > ₹7,500
        oldSnap.setInputsJson(Map.of("societyGstRegistered", true, "societyTurnoverPaise", 300_000_000L, "flatNumber", "A-101"));

        when(ruleRepo.findById(newRuleId)).thenReturn(Optional.of(raisedThresholdRule));
        when(snapRepo.findBySocietyIdAndFyCodeAndRuleFamilyAndSupplyDateBetweenOrderBySupplyDateAsc(
                eq(1L), eq("2026-27"), eq(StatutoryTaxRuleService.FAMILY_GST_MAINTENANCE), any(), any()
        )).thenReturn(List.of(oldSnap));

        RecalculationBatchDto batch = service.recalculateRange(new RecalculateTaxRequest(
                1L, "2026-27", StatutoryTaxRuleService.FAMILY_GST_MAINTENANCE,
                newRuleId.toString(), "2026-04-01", "2026-11-28"
        ));

        assertEquals(1, batch.evaluatedCount());
        assertEquals(1, batch.adjustedCount());
        assertEquals(-153_000L, batch.netDeltaPaise());
        assertEquals("CREDIT_NOTE", batch.notes().get(0).noteType());
        assertEquals(153_000L, oldSnap.getComputedTaxPaise()); // original snapshot untouched
    }
}
