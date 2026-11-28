package com.manacommunity.api.clubhousepos.controller;

import com.manacommunity.api.clubhousepos.entity.*;
import com.manacommunity.api.clubhousepos.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clubhouse-pos-test")
public class ClubhousePosTestController {

    private final PosTabAuthTestRepository posRepo;
    private final GuestPassQuotaTestRepository guestRepo;
    private final EvChargerOcppTestRepository evRepo;

    public ClubhousePosTestController(PosTabAuthTestRepository posRepo,
                                       GuestPassQuotaTestRepository guestRepo,
                                       EvChargerOcppTestRepository evRepo) {
        this.posRepo = posRepo;
        this.guestRepo = guestRepo;
        this.evRepo = evRepo;
    }

    public record ClubhousePosDashboard(int totalTests, int passed, int failed, int pending, double passRate,
                                         List<PosTabAuthTest> posTests, List<GuestPassQuotaTest> guestTests,
                                         List<EvChargerOcppTest> evTests) {}

    @GetMapping("/dashboard")
    public ResponseEntity<ClubhousePosDashboard> getDashboard() {
        var p = posRepo.findAll(); var g = guestRepo.findAll(); var e = evRepo.findAll();
        int total = p.size() + g.size() + e.size();
        if (total == 0) return seedDemo();
        long pass = p.stream().filter(t -> t.getTestResult() == PosTabAuthTest.TestResult.PASS).count()
                + g.stream().filter(t -> t.getTestResult() == GuestPassQuotaTest.TestResult.PASS).count()
                + e.stream().filter(t -> t.getTestResult() == EvChargerOcppTest.TestResult.PASS).count();
        return ResponseEntity.ok(new ClubhousePosDashboard(total, (int) pass, (int)(total - pass), 0, Math.round(pass * 1000.0 / total) / 10.0, p, g, e));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<ClubhousePosDashboard> seedDemo() {
        var p1 = PosTabAuthTest.builder().communityId("COMM-001").testCaseCode("POS-001").testCaseName("Café order ₹380 billed to Tower B-804 within limit")
                .scenario(PosTabAuthTest.PosScenario.FLAT_TAB_ORDER_WITHIN_LIMIT)
                .flatNumber("Tower B - 804").orderDescription("2x Cappuccino + Club Sandwich")
                .orderAmount(380).flatCreditLimit(5000).currentTabBalance(1200)
                .creditLimitExceeded(false).authMethod("PIN").pinVerified(true)
                .pushApprovalSent(false).pushApprovalReceived(false)
                .batchReconciled(true).batchTotal(12400).ledgerTotal(12400).ledgerMatchesBatch(true)
                .testResult(PosTabAuthTest.TestResult.PASS).failureReason("").build();

        var p2 = PosTabAuthTest.builder().communityId("COMM-001").testCaseCode("POS-002").testCaseName("Credit limit exceeded — order not blocked")
                .scenario(PosTabAuthTest.PosScenario.CREDIT_LIMIT_EXCEEDED_BLOCKED)
                .flatNumber("Tower A - 302").orderDescription("Party order 15 items")
                .orderAmount(4800).flatCreditLimit(5000).currentTabBalance(4600)
                .creditLimitExceeded(true).authMethod("PIN").pinVerified(true)
                .pushApprovalSent(false).pushApprovalReceived(false)
                .batchReconciled(false).batchTotal(0).ledgerTotal(0).ledgerMatchesBatch(false)
                .testResult(PosTabAuthTest.TestResult.FAIL)
                .failureReason("Tab balance ₹4,600 + order ₹4,800 = ₹9,400 exceeds ₹5,000 limit — credit check bypassed at POS").build();

        var p3 = PosTabAuthTest.builder().communityId("COMM-001").testCaseCode("POS-003").testCaseName("Daily batch reconciliation — ₹240 mismatch")
                .scenario(PosTabAuthTest.PosScenario.BATCH_LEDGER_MISMATCH)
                .flatNumber("ALL").orderDescription("End-of-day batch")
                .orderAmount(0).flatCreditLimit(0).currentTabBalance(0)
                .creditLimitExceeded(false).authMethod("").pinVerified(false)
                .pushApprovalSent(false).pushApprovalReceived(false)
                .batchReconciled(false).batchTotal(18640).ledgerTotal(18400).ledgerMatchesBatch(false)
                .testResult(PosTabAuthTest.TestResult.FAIL)
                .failureReason("Batch total ₹18,640 vs ledger ₹18,400 — ₹240 discrepancy from 2 cancelled orders not reversed").build();

        var g1 = GuestPassQuotaTest.builder().communityId("COMM-001").testCaseCode("GST-001").testCaseName("2nd free guest pass — pool, within quota")
                .scenario(GuestPassQuotaTest.GuestScenario.FREE_QUOTA_WITHIN_LIMIT)
                .amenityName("Swimming Pool").flatNumber("Tower C - 1201")
                .freeQuota(2).usedQuota(1).quotaExceeded(false)
                .weekdayPrice(150).weekendPrice(300).chargedAmount(0).isWeekend(false)
                .turnstileQrGenerated(true).qrSingleUseEnforced(true).qrExpired(false).qrExpiryMinutes(30)
                .testResult(GuestPassQuotaTest.TestResult.PASS).failureReason("").build();

        var g2 = GuestPassQuotaTest.builder().communityId("COMM-001").testCaseCode("GST-002").testCaseName("3rd guest weekend — ₹300 charged correctly")
                .scenario(GuestPassQuotaTest.GuestScenario.QUOTA_EXCEEDED_WEEKEND_CHARGE)
                .amenityName("Swimming Pool").flatNumber("Tower C - 1201")
                .freeQuota(2).usedQuota(2).quotaExceeded(true)
                .weekdayPrice(150).weekendPrice(300).chargedAmount(300).isWeekend(true)
                .turnstileQrGenerated(true).qrSingleUseEnforced(true).qrExpired(false).qrExpiryMinutes(30)
                .testResult(GuestPassQuotaTest.TestResult.PASS).failureReason("").build();

        var g3 = GuestPassQuotaTest.builder().communityId("COMM-001").testCaseCode("GST-003").testCaseName("QR code reused at turnstile — not blocked")
                .scenario(GuestPassQuotaTest.GuestScenario.DOUBLE_SCAN_SAME_QR)
                .amenityName("Gym").flatNumber("Tower A - 505")
                .freeQuota(2).usedQuota(0).quotaExceeded(false)
                .weekdayPrice(150).weekendPrice(300).chargedAmount(0).isWeekend(false)
                .turnstileQrGenerated(true).qrSingleUseEnforced(false).qrExpired(false).qrExpiryMinutes(30)
                .testResult(GuestPassQuotaTest.TestResult.FAIL)
                .failureReason("Same QR scanned twice at gym turnstile — single-use flag not enforced, 2 entries on 1 pass").build();

        var e1 = EvChargerOcppTest.builder().communityId("COMM-001").testCaseCode("EVC-001").testCaseName("Normal charge session — 12.4 kWh billed correctly")
                .scenario(EvChargerOcppTest.OcppScenario.NORMAL_CHARGE_SESSION)
                .chargerId("EV-PARK-B1-03").flatNumber("Tower B - 804").vehicleNumber("KA-01-EV-5678")
                .kwhConsumed(12.4).ratePerKwh(8.5).totalCharge(105.4).walletBalance(500)
                .walletDeducted(true).sessionStarted(true).sessionStopped(true)
                .powerCutDuringCharge(false).sessionResumedAfterPowerCut(false)
                .socPercent(100).overstayDetected(false).overstayMinutes(0).overstayPenalty(0)
                .testResult(EvChargerOcppTest.TestResult.PASS).failureReason("").build();

        var e2 = EvChargerOcppTest.builder().communityId("COMM-001").testCaseCode("EVC-002").testCaseName("Power cut mid-charge — session not resumed")
                .scenario(EvChargerOcppTest.OcppScenario.POWER_CUT_MID_CHARGE_RESUME)
                .chargerId("EV-PARK-B2-01").flatNumber("Tower A - 302").vehicleNumber("MH-02-EV-1234")
                .kwhConsumed(6.2).ratePerKwh(8.5).totalCharge(52.7).walletBalance(300)
                .walletDeducted(true).sessionStarted(true).sessionStopped(false)
                .powerCutDuringCharge(true).sessionResumedAfterPowerCut(false)
                .socPercent(45).overstayDetected(false).overstayMinutes(0).overstayPenalty(0)
                .testResult(EvChargerOcppTest.TestResult.FAIL)
                .failureReason("OCPP session not resumed after 8-min power cut — car stuck at 45% SoC, billed for 6.2 kWh but charge incomplete").build();

        var e3 = EvChargerOcppTest.builder().communityId("COMM-001").testCaseCode("EVC-003").testCaseName("Overstay 45 min after 100% — penalty not applied")
                .scenario(EvChargerOcppTest.OcppScenario.OVERSTAY_PENALTY_AFTER_100_PERCENT)
                .chargerId("EV-PARK-B1-03").flatNumber("Tower C - 1201").vehicleNumber("KA-05-EV-9999")
                .kwhConsumed(18.6).ratePerKwh(8.5).totalCharge(158.1).walletBalance(800)
                .walletDeducted(true).sessionStarted(true).sessionStopped(false)
                .powerCutDuringCharge(false).sessionResumedAfterPowerCut(false)
                .socPercent(100).overstayDetected(true).overstayMinutes(45).overstayPenalty(0)
                .testResult(EvChargerOcppTest.TestResult.FAIL)
                .failureReason("Car at 100% SoC for 45 min — overstay penalty ₹5/min not triggered, charger blocked for others").build();

        posRepo.saveAll(List.of(p1, p2, p3));
        guestRepo.saveAll(List.of(g1, g2, g3));
        evRepo.saveAll(List.of(e1, e2, e3));

        return ResponseEntity.ok(new ClubhousePosDashboard(9, 4, 5, 0, 44.4,
                List.of(p1, p2, p3), List.of(g1, g2, g3), List.of(e1, e2, e3)));
    }
}
