package com.manacommunity.api.guardpayroll.controller;

import com.manacommunity.api.guardpayroll.entity.*;
import com.manacommunity.api.guardpayroll.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guard-payroll-test")
public class GuardPayrollTestController {

    private final ShiftHandoverTestRepository shiftRepo;
    private final AttendancePayrollTestRepository payrollRepo;
    private final PatrolComplianceTestRepository patrolRepo;

    public GuardPayrollTestController(ShiftHandoverTestRepository shiftRepo,
                                       AttendancePayrollTestRepository payrollRepo,
                                       PatrolComplianceTestRepository patrolRepo) {
        this.shiftRepo = shiftRepo;
        this.payrollRepo = payrollRepo;
        this.patrolRepo = patrolRepo;
    }

    public record GuardPayrollDashboard(int totalTests, int passed, int failed, int pending, double passRate,
                                         List<ShiftHandoverTest> shiftTests, List<AttendancePayrollTest> payrollTests,
                                         List<PatrolComplianceTest> patrolTests) {}

    @GetMapping("/dashboard")
    public ResponseEntity<GuardPayrollDashboard> getDashboard() {
        var s = shiftRepo.findAll(); var p = payrollRepo.findAll(); var r = patrolRepo.findAll();
        int total = s.size() + p.size() + r.size();
        if (total == 0) return ResponseEntity.ok(seedDemo());
        long pass = s.stream().filter(t -> t.getTestResult() == ShiftHandoverTest.TestResult.PASS).count()
                + p.stream().filter(t -> t.getTestResult() == AttendancePayrollTest.TestResult.PASS).count()
                + r.stream().filter(t -> t.getTestResult() == PatrolComplianceTest.TestResult.PASS).count();
        return ResponseEntity.ok(new GuardPayrollDashboard(total, (int) pass, (int)(total - pass), 0, Math.round(pass * 1000.0 / total) / 10.0, s, p, r));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<GuardPayrollDashboard> seedDemo() {
        var s1 = ShiftHandoverTest.builder().communityId("COMM-001").testCaseCode("SHF-001").testCaseName("Clean 8 AM shift change — full checklist")
                .scenario(ShiftHandoverTest.HandoverScenario.CLEAN_SHIFT_CHANGE_8AM)
                .outgoingGuard("Raju Kumar").incomingGuard("Suresh Yadav").shiftType("Night → Day")
                .scheduledHandoverTime("08:00").actualHandoverTime("08:03").delayMinutes(3)
                .checklistCompleted(true).checklistItems(12).itemsVerified(12)
                .keyHandoverLogged(true).patrolRouteTransferred(true).incidentLogReviewed(true).gpsLocationVerified(true)
                .testResult(ShiftHandoverTest.TestResult.PASS).failureReason("").build();

        var s2 = ShiftHandoverTest.builder().communityId("COMM-001").testCaseCode("SHF-002").testCaseName("Key handover not logged in system")
                .scenario(ShiftHandoverTest.HandoverScenario.KEY_HANDOVER_NOT_LOGGED)
                .outgoingGuard("Vinod Singh").incomingGuard("Prakash Sharma").shiftType("Day → Evening")
                .scheduledHandoverTime("16:00").actualHandoverTime("16:12").delayMinutes(12)
                .checklistCompleted(true).checklistItems(12).itemsVerified(10)
                .keyHandoverLogged(false).patrolRouteTransferred(true).incidentLogReviewed(true).gpsLocationVerified(true)
                .testResult(ShiftHandoverTest.TestResult.FAIL)
                .failureReason("Physical keys exchanged but handover not logged — audit trail gap, 2 master keys unaccounted").build();

        var s3 = ShiftHandoverTest.builder().communityId("COMM-001").testCaseCode("SHF-003").testCaseName("GPS spoof detected during handover location check")
                .scenario(ShiftHandoverTest.HandoverScenario.GPS_SPOOF_AT_HANDOVER)
                .outgoingGuard("Deepak Patel").incomingGuard("Ramesh Gupta").shiftType("Evening → Night")
                .scheduledHandoverTime("00:00").actualHandoverTime("00:05").delayMinutes(5)
                .checklistCompleted(true).checklistItems(12).itemsVerified(12)
                .keyHandoverLogged(true).patrolRouteTransferred(true).incidentLogReviewed(true).gpsLocationVerified(false)
                .testResult(ShiftHandoverTest.TestResult.FAIL)
                .failureReason("GPS location 2.3 km from society gate — mock location app detected on guard device").build();

        var p1 = AttendancePayrollTest.builder().communityId("COMM-001").testCaseCode("PAY-001").testCaseName("Monthly salary calculation — 26 days present")
                .scenario(AttendancePayrollTest.PayrollScenario.MONTHLY_SALARY_CORRECT)
                .guardName("Raju Kumar").agencyName("SecureGuard Services")
                .totalShifts(30).presentDays(26).absentDays(4).overtimeHours(8)
                .basicSalary(15000).overtimePay(1500).deductions(2600).netPay(13900)
                .esicApplied(true).pfDeducted(true).esicAmount(600).pfAmount(1800)
                .payslipGenerated(true).paymentMode("Bank Transfer")
                .testResult(AttendancePayrollTest.TestResult.PASS).failureReason("").build();

        var p2 = AttendancePayrollTest.builder().communityId("COMM-001").testCaseCode("PAY-002").testCaseName("Overtime at double rate — not applied")
                .scenario(AttendancePayrollTest.PayrollScenario.OVERTIME_CALCULATION_DOUBLE_RATE)
                .guardName("Suresh Yadav").agencyName("SecureGuard Services")
                .totalShifts(30).presentDays(30).absentDays(0).overtimeHours(16)
                .basicSalary(15000).overtimePay(1600).deductions(2400).netPay(14200)
                .esicApplied(true).pfDeducted(true).esicAmount(600).pfAmount(1800)
                .payslipGenerated(true).paymentMode("Bank Transfer")
                .testResult(AttendancePayrollTest.TestResult.FAIL)
                .failureReason("16 hrs OT calculated at 1x rate (₹1,600) instead of 2x (₹3,200) — guard underpaid ₹1,600").build();

        var p3 = AttendancePayrollTest.builder().communityId("COMM-001").testCaseCode("PAY-003").testCaseName("ESIC + PF deduction verification")
                .scenario(AttendancePayrollTest.PayrollScenario.ESIC_PF_DEDUCTION_CHECK)
                .guardName("Vinod Singh").agencyName("AllSafe Securities")
                .totalShifts(30).presentDays(28).absentDays(2).overtimeHours(0)
                .basicSalary(18000).overtimePay(0).deductions(3240).netPay(14760)
                .esicApplied(true).pfDeducted(true).esicAmount(1440).pfAmount(2160)
                .payslipGenerated(true).paymentMode("Bank Transfer")
                .testResult(AttendancePayrollTest.TestResult.PASS).failureReason("").build();

        var r1 = PatrolComplianceTest.builder().communityId("COMM-001").testCaseCode("PTR-001").testCaseName("All 8 checkpoints scanned in sequence")
                .scenario(PatrolComplianceTest.PatrolScenario.ALL_CHECKPOINTS_SCANNED)
                .guardName("Raju Kumar").routeName("Perimeter Route A")
                .totalCheckpoints(8).scannedCheckpoints(8).missedCheckpoints(0)
                .gpsTrackingEnabled(true).routeDeviationDetected(false).deviationMeters(0)
                .scanMethod("NFC").nightVisionPhotoTaken(true).panicButtonTested(false)
                .patrolDurationMinutes(42).expectedDurationMinutes(45)
                .testResult(PatrolComplianceTest.TestResult.PASS).failureReason("").build();

        var r2 = PatrolComplianceTest.builder().communityId("COMM-001").testCaseCode("PTR-002").testCaseName("Checkpoint 5 missed — no alert generated")
                .scenario(PatrolComplianceTest.PatrolScenario.MISSED_CHECKPOINT_ALERT)
                .guardName("Deepak Patel").routeName("Basement Route B")
                .totalCheckpoints(6).scannedCheckpoints(5).missedCheckpoints(1)
                .gpsTrackingEnabled(true).routeDeviationDetected(true).deviationMeters(85)
                .scanMethod("QR").nightVisionPhotoTaken(false).panicButtonTested(false)
                .patrolDurationMinutes(28).expectedDurationMinutes(35)
                .testResult(PatrolComplianceTest.TestResult.FAIL)
                .failureReason("Checkpoint 5 (Basement B2 stairwell) skipped — alert not sent to supervisor, patrol marked complete").build();

        shiftRepo.saveAll(List.of(s1, s2, s3));
        payrollRepo.saveAll(List.of(p1, p2, p3));
        patrolRepo.saveAll(List.of(r1, r2));

        return ResponseEntity.ok(new GuardPayrollDashboard(8, 4, 4, 0, 50.0,
                List.of(s1, s2, s3), List.of(p1, p2, p3), List.of(r1, r2)));
    }
}
