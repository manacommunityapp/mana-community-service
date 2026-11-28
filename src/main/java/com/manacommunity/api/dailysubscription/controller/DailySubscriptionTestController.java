package com.manacommunity.api.dailysubscription.controller;

import com.manacommunity.api.dailysubscription.entity.*;
import com.manacommunity.api.dailysubscription.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/daily-subscription-test")
public class DailySubscriptionTestController {

    private final CutoffVacationTestRepository cutoffRepo;
    private final WalletRefundTestRepository walletRepo;
    private final RiderBatchingTestRepository riderRepo;

    public DailySubscriptionTestController(CutoffVacationTestRepository cutoffRepo,
                                            WalletRefundTestRepository walletRepo,
                                            RiderBatchingTestRepository riderRepo) {
        this.cutoffRepo = cutoffRepo;
        this.walletRepo = walletRepo;
        this.riderRepo = riderRepo;
    }

    public record SubscriptionDashboard(int totalTests, int passed, int failed, int pending, double passRate,
                                         List<CutoffVacationTest> cutoffTests, List<WalletRefundTest> walletTests,
                                         List<RiderBatchingTest> riderTests) {}

    @GetMapping("/dashboard")
    public ResponseEntity<SubscriptionDashboard> getDashboard() {
        var c = cutoffRepo.findAll(); var w = walletRepo.findAll(); var r = riderRepo.findAll();
        int total = c.size() + w.size() + r.size();
        if (total == 0) return seedDemo();
        long pass = c.stream().filter(t -> t.getTestResult() == CutoffVacationTest.TestResult.PASS).count()
                + w.stream().filter(t -> t.getTestResult() == WalletRefundTest.TestResult.PASS).count()
                + r.stream().filter(t -> t.getTestResult() == RiderBatchingTest.TestResult.PASS).count();
        long fail = total - pass;
        return ResponseEntity.ok(new SubscriptionDashboard(total, (int) pass, (int) fail, 0, Math.round(pass * 1000.0 / total) / 10.0, c, w, r));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<SubscriptionDashboard> seedDemo() {
        var c1 = CutoffVacationTest.builder().communityId("COMM-001").testCaseCode("CUT-001").testCaseName("Order at 11:59:59 PM — before cutoff")
                .scenario(CutoffVacationTest.CutoffScenario.CUTOFF_BOUNDARY_11_59_59).subscriptionType("Milk").vendorName("Country Delight")
                .cutoffTime("00:00:00").orderModificationTime("23:59:59").beforeCutoff(true).modificationAccepted(true)
                .vacationModeEnabled(false).skippedDays(0).skippedDaysBilled(false).billingCorrect(true)
                .expectedAmount(68.0).actualAmount(68.0).varianceAmount(0).testResult(CutoffVacationTest.TestResult.PASS).failureReason("").build();

        var c2 = CutoffVacationTest.builder().communityId("COMM-001").testCaseCode("CUT-002").testCaseName("Order at 12:00:01 AM — after cutoff")
                .scenario(CutoffVacationTest.CutoffScenario.CUTOFF_BOUNDARY_12_00_01).subscriptionType("Milk").vendorName("Country Delight")
                .cutoffTime("00:00:00").orderModificationTime("00:00:01").beforeCutoff(false).modificationAccepted(true)
                .vacationModeEnabled(false).skippedDays(0).skippedDaysBilled(false).billingCorrect(false)
                .expectedAmount(0).actualAmount(68.0).varianceAmount(68.0).testResult(CutoffVacationTest.TestResult.FAIL)
                .failureReason("Order modification accepted 1 second after cutoff — should have been rejected").build();

        var c3 = CutoffVacationTest.builder().communityId("COMM-001").testCaseCode("CUT-003").testCaseName("Vacation pause 5 days — billing check")
                .scenario(CutoffVacationTest.CutoffScenario.VACATION_PAUSE_RESUME).subscriptionType("Bread").vendorName("BB Daily")
                .cutoffTime("22:00:00").orderModificationTime("").beforeCutoff(true).modificationAccepted(true)
                .vacationModeEnabled(true).vacationStartDate("2026-12-01").vacationEndDate("2026-12-05").skippedDays(5).skippedDaysBilled(false)
                .billingCorrect(true).expectedAmount(780.0).actualAmount(780.0).varianceAmount(0)
                .testResult(CutoffVacationTest.TestResult.PASS).failureReason("").build();

        var c4 = CutoffVacationTest.builder().communityId("COMM-001").testCaseCode("CUT-004").testCaseName("Modification during dispatch window")
                .scenario(CutoffVacationTest.CutoffScenario.MODIFICATION_DURING_DISPATCH).subscriptionType("Newspaper").vendorName("Local Vendor")
                .cutoffTime("04:00:00").orderModificationTime("04:45:00").beforeCutoff(false).modificationAccepted(false)
                .vacationModeEnabled(false).skippedDays(0).skippedDaysBilled(false).billingCorrect(true)
                .expectedAmount(12.0).actualAmount(12.0).varianceAmount(0)
                .testResult(CutoffVacationTest.TestResult.PASS).failureReason("").build();

        var w1 = WalletRefundTest.builder().communityId("COMM-001").testCaseCode("WAL-001").testCaseName("Low wallet ₹15 before ₹68 batch run")
                .scenario(WalletRefundTest.WalletScenario.LOW_BALANCE_BEFORE_BATCH)
                .walletBalanceBefore(15.0).walletBalanceAfter(15.0).orderAmount(68.0).refundAmount(0)
                .batchRunTime("05:00:00").lowBalanceAlertSent(true).subscriptionPaused(true).autoRefundTriggered(false).refundDelayMs(0)
                .vendorAction("SKIP").refundReason("")
                .testResult(WalletRefundTest.TestResult.PASS).failureReason("").build();

        var w2 = WalletRefundTest.builder().communityId("COMM-001").testCaseCode("WAL-002").testCaseName("Out-of-stock instant refund")
                .scenario(WalletRefundTest.WalletScenario.OUT_OF_STOCK_INSTANT_REFUND)
                .walletBalanceBefore(500.0).walletBalanceAfter(568.0).orderAmount(68.0).refundAmount(68.0)
                .batchRunTime("05:00:00").lowBalanceAlertSent(false).subscriptionPaused(false).autoRefundTriggered(true).refundDelayMs(1200)
                .vendorAction("OUT_OF_STOCK").refundReason("Vendor marked Toned Milk 500ml out of stock")
                .testResult(WalletRefundTest.TestResult.PASS).failureReason("").build();

        var w3 = WalletRefundTest.builder().communityId("COMM-001").testCaseCode("WAL-003").testCaseName("Concurrent debit race condition")
                .scenario(WalletRefundTest.WalletScenario.CONCURRENT_DEBIT_RACE_CONDITION)
                .walletBalanceBefore(100.0).walletBalanceAfter(-36.0).orderAmount(68.0).refundAmount(0)
                .batchRunTime("05:00:00").lowBalanceAlertSent(false).subscriptionPaused(false).autoRefundTriggered(false).refundDelayMs(0)
                .vendorAction("DELIVERED").refundReason("")
                .testResult(WalletRefundTest.TestResult.FAIL)
                .failureReason("Wallet went negative ₹-36 — two debits (milk ₹68 + bread ₹68) processed without atomic lock").build();

        var r1 = RiderBatchingTest.builder().communityId("COMM-001").testCaseCode("RDR-001").testCaseName("Single rider 15 packages — batch check-in")
                .scenario(RiderBatchingTest.BatchScenario.SINGLE_RIDER_15_PACKAGES)
                .riderName("Ravi Kumar").totalPackages(15).flatsToDeliver(15).batchCheckInCompleted(true)
                .individualNotificationsSent(15).notificationsDelivered(14).notificationsFailed(1)
                .partialExitAllowed(false).packagesDelivered(15).packagesReturned(0).reconciliationCorrect(true).reconciliationMismatch("")
                .testResult(RiderBatchingTest.TestResult.PASS).failureReason("").build();

        var r2 = RiderBatchingTest.builder().communityId("COMM-001").testCaseCode("RDR-002").testCaseName("Partial delivery — 3 returned, exit reconciliation")
                .scenario(RiderBatchingTest.BatchScenario.PARTIAL_DELIVERY_EXIT)
                .riderName("Suresh Babu").totalPackages(12).flatsToDeliver(12).batchCheckInCompleted(true)
                .individualNotificationsSent(12).notificationsDelivered(12).notificationsFailed(0)
                .partialExitAllowed(true).packagesDelivered(9).packagesReturned(3).reconciliationCorrect(false)
                .reconciliationMismatch("Guard logged 2 returned but rider had 3 — 1 package unaccounted")
                .testResult(RiderBatchingTest.TestResult.FAIL)
                .failureReason("Exit reconciliation mismatch: 1 package unaccounted at gate").build();

        cutoffRepo.saveAll(List.of(c1, c2, c3, c4));
        walletRepo.saveAll(List.of(w1, w2, w3));
        riderRepo.saveAll(List.of(r1, r2));

        return ResponseEntity.ok(new SubscriptionDashboard(9, 6, 3, 0, 66.7,
                List.of(c1, c2, c3, c4), List.of(w1, w2, w3), List.of(r1, r2)));
    }
}
