package com.manacommunity.api.builderhandover.controller;

import com.manacommunity.api.builderhandover.entity.*;
import com.manacommunity.api.builderhandover.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/builder-handover-test")
public class BuilderHandoverTestController {

    private final SnagListTestRepository snagRepo;
    private final CorpusFundTestRepository corpusRepo;

    public BuilderHandoverTestController(SnagListTestRepository snagRepo, CorpusFundTestRepository corpusRepo) {
        this.snagRepo = snagRepo;
        this.corpusRepo = corpusRepo;
    }

    public record BuilderHandoverDashboard(int totalTests, int passed, int failed, int pending, double passRate,
                                            List<SnagListTest> snagTests, List<CorpusFundTest> corpusTests) {}

    @GetMapping("/dashboard")
    public ResponseEntity<BuilderHandoverDashboard> getDashboard() {
        var s = snagRepo.findAll(); var c = corpusRepo.findAll();
        int total = s.size() + c.size();
        if (total == 0) return seedDemo();
        long pass = s.stream().filter(t -> t.getTestResult() == SnagListTest.TestResult.PASS).count()
                + c.stream().filter(t -> t.getTestResult() == CorpusFundTest.TestResult.PASS).count();
        return ResponseEntity.ok(new BuilderHandoverDashboard(total, (int) pass, (int)(total - pass), 0, Math.round(pass * 1000.0 / total) / 10.0, s, c));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<BuilderHandoverDashboard> seedDemo() {
        var s1 = SnagListTest.builder().communityId("COMM-001").testCaseCode("SNG-001").testCaseName("Cracked tile logged with photo annotation")
                .scenario(SnagListTest.SnagScenario.CRACK_TILE_LOGGED_WITH_PHOTO)
                .flatNumber("Tower A - 302").roomName("Master Bedroom").defectType("Cracked floor tile")
                .severity("Medium").photoAnnotated(true).assignedContractor("RK Tiles & Flooring")
                .contractorTrade("Civil").slaDays(7).actualDays(5).slaBreached(false)
                .reinspectionDone(true).signedOff(true).possessionBlocked(false)
                .testResult(SnagListTest.TestResult.PASS).failureReason("").build();

        var s2 = SnagListTest.builder().communityId("COMM-001").testCaseCode("SNG-002").testCaseName("Seepage high severity — possession not blocked")
                .scenario(SnagListTest.SnagScenario.SEEPAGE_HIGH_SEVERITY_BLOCKS_POSSESSION)
                .flatNumber("Tower B - 1105").roomName("Bathroom").defectType("Wall seepage behind shower")
                .severity("High").photoAnnotated(true).assignedContractor("Waterproof Solutions Ltd")
                .contractorTrade("Plumbing").slaDays(14).actualDays(0).slaBreached(false)
                .reinspectionDone(false).signedOff(false).possessionBlocked(false)
                .testResult(SnagListTest.TestResult.FAIL)
                .failureReason("High-severity seepage snag open but possession certificate generated — blocking rule not enforced").build();

        var s3 = SnagListTest.builder().communityId("COMM-001").testCaseCode("SNG-003").testCaseName("Electrical fault — SLA 10 days breached, no escalation")
                .scenario(SnagListTest.SnagScenario.SLA_BREACH_NO_ESCALATION)
                .flatNumber("Tower C - 804").roomName("Kitchen").defectType("Dead power outlet near chimney")
                .severity("Medium").photoAnnotated(true).assignedContractor("Bright Electricals")
                .contractorTrade("Electrical").slaDays(10).actualDays(18).slaBreached(true)
                .reinspectionDone(false).signedOff(false).possessionBlocked(false)
                .testResult(SnagListTest.TestResult.FAIL)
                .failureReason("SLA breached by 8 days — no auto-escalation to project manager, contractor not penalized").build();

        var s4 = SnagListTest.builder().communityId("COMM-001").testCaseCode("SNG-004").testCaseName("Re-inspection loop — 3rd attempt sign-off")
                .scenario(SnagListTest.SnagScenario.REINSPECTION_LOOP_3_ATTEMPTS)
                .flatNumber("Tower A - 607").roomName("Living Room").defectType("Uneven wall plastering")
                .severity("Low").photoAnnotated(true).assignedContractor("BuildRight Contractors")
                .contractorTrade("Civil").slaDays(7).actualDays(21).slaBreached(true)
                .reinspectionDone(true).signedOff(true).possessionBlocked(false)
                .testResult(SnagListTest.TestResult.PASS).failureReason("").build();

        var c1 = CorpusFundTest.builder().communityId("COMM-001").testCaseCode("CRP-001").testCaseName("Opening balance locked at handover")
                .scenario(CorpusFundTest.CorpusScenario.OPENING_BALANCE_LOCKED)
                .openingBalance(4500000).corpusFundAmount(12000000).gstInputCredit(180000)
                .escrowDisputedAmount(0).balanceLocked(true).gstCarryoverCorrect(true)
                .escrowTracked(true).pendingVendorBills(0).pendingBillAmount(0)
                .handoverSnapshotGenerated(true).handoverDate("2024-03-15").rwaAccepted(true)
                .testResult(CorpusFundTest.TestResult.PASS).failureReason("").build();

        var c2 = CorpusFundTest.builder().communityId("COMM-001").testCaseCode("CRP-002").testCaseName("GST input credit ₹1.8L not carried over to RWA")
                .scenario(CorpusFundTest.CorpusScenario.GST_INPUT_CREDIT_CARRYOVER)
                .openingBalance(4500000).corpusFundAmount(12000000).gstInputCredit(180000)
                .escrowDisputedAmount(0).balanceLocked(true).gstCarryoverCorrect(false)
                .escrowTracked(true).pendingVendorBills(0).pendingBillAmount(0)
                .handoverSnapshotGenerated(true).handoverDate("2024-03-15").rwaAccepted(false)
                .testResult(CorpusFundTest.TestResult.FAIL)
                .failureReason("GST input credit ₹1,80,000 not transferred to RWA GSTIN — builder retained credit post-handover").build();

        var c3 = CorpusFundTest.builder().communityId("COMM-001").testCaseCode("CRP-003").testCaseName("3 disputed vendor bills in escrow — ₹4.2L")
                .scenario(CorpusFundTest.CorpusScenario.ESCROW_DISPUTED_VENDOR_BILLS)
                .openingBalance(4500000).corpusFundAmount(12000000).gstInputCredit(180000)
                .escrowDisputedAmount(420000).balanceLocked(true).gstCarryoverCorrect(true)
                .escrowTracked(true).pendingVendorBills(3).pendingBillAmount(420000)
                .handoverSnapshotGenerated(true).handoverDate("2024-03-15").rwaAccepted(true)
                .testResult(CorpusFundTest.TestResult.PASS).failureReason("").build();

        snagRepo.saveAll(List.of(s1, s2, s3, s4));
        corpusRepo.saveAll(List.of(c1, c2, c3));

        return ResponseEntity.ok(new BuilderHandoverDashboard(7, 4, 3, 0, 57.1,
                List.of(s1, s2, s3, s4), List.of(c1, c2, c3)));
    }
}
