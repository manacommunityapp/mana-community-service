package com.manacommunity.api.staffpayroll.controller;

import com.manacommunity.api.staffpayroll.entity.*;
import com.manacommunity.api.staffpayroll.entity.AttendancePunch.*;
import com.manacommunity.api.staffpayroll.entity.DomesticStaffProfile.StaffRole;
import com.manacommunity.api.staffpayroll.entity.EmployerContract.*;
import com.manacommunity.api.staffpayroll.entity.SalaryAdvance.*;
import com.manacommunity.api.staffpayroll.repository.*;
import com.manacommunity.api.staffpayroll.service.AdvanceAmortizationService;
import com.manacommunity.api.staffpayroll.service.ProRataWageEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/staff-payroll")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StaffPayrollController {

    private final ProRataWageEngine wageEngine;
    private final AdvanceAmortizationService advanceService;
    private final DomesticStaffProfileRepository staffRepo;
    private final EmployerContractRepository contractRepo;
    private final AttendancePunchRepository punchRepo;
    private final PayrollLedgerRepository payrollRepo;
    private final SalaryAdvanceRepository advanceRepo;
    private final AdvanceInstallmentRepository installmentRepo;

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        return ResponseEntity.ok(wageEngine.getDashboard());
    }

    @GetMapping("/advances/dashboard")
    public ResponseEntity<?> getAdvanceDashboard() {
        return ResponseEntity.ok(advanceService.getAdvanceDashboard());
    }

    @PostMapping("/payroll/calculate")
    public ResponseEntity<?> calculatePayroll(@RequestParam Long staffId, @RequestParam String month) {
        return ResponseEntity.ok(wageEngine.calculateMonthlyPayroll(staffId, month));
    }

    @PostMapping("/advances/disburse")
    public ResponseEntity<?> disburseAdvance(@RequestParam Long staffId, @RequestParam String staffName,
                                               @RequestParam Long contractId, @RequestParam double amount,
                                               @RequestParam int installments, @RequestParam String method,
                                               @RequestParam(required = false) String upiTxnId,
                                               @RequestParam(required = false) String purpose) {
        return ResponseEntity.ok(advanceService.disburseAdvance(staffId, staffName, contractId, amount, installments,
                PaymentMethod.valueOf(method), upiTxnId, purpose));
    }

    @PostMapping("/advances/{advanceId}/deduct/{installmentNumber}")
    public ResponseEntity<?> deductInstallment(@PathVariable Long advanceId, @PathVariable int installmentNumber) {
        return ResponseEntity.ok(advanceService.deductInstallment(advanceId, installmentNumber));
    }

    @PostMapping("/advances/{advanceId}/terminate")
    public ResponseEntity<?> terminateAdvance(@PathVariable Long advanceId) {
        return ResponseEntity.ok(advanceService.handleMidMonthTermination(advanceId));
    }

    @GetMapping("/advances/{advanceId}/schedule")
    public ResponseEntity<?> getAmortizationSchedule(@PathVariable Long advanceId) {
        return ResponseEntity.ok(advanceService.getAmortizationSchedule(advanceId));
    }

    @GetMapping("/staff")
    public ResponseEntity<?> getAllStaff() {
        return ResponseEntity.ok(staffRepo.findAll());
    }

    @GetMapping("/attendance/{staffId}")
    public ResponseEntity<?> getAttendance(@PathVariable Long staffId,
                                             @RequestParam String from, @RequestParam String to) {
        return ResponseEntity.ok(punchRepo.findByStaffIdAndPunchDateBetween(staffId,
                LocalDate.parse(from), LocalDate.parse(to)));
    }

    @GetMapping("/payroll/{staffId}")
    public ResponseEntity<?> getPayroll(@PathVariable Long staffId, @RequestParam String month) {
        return ResponseEntity.ok(payrollRepo.findByStaffIdAndPayrollMonth(staffId, month));
    }

    @PostMapping("/seed-demo")
    public ResponseEntity<?> seedDemoData() {
        // Staff 1: Sunita — Maid working at 3 flats with different salary cycles
        DomesticStaffProfile sunita = staffRepo.save(DomesticStaffProfile.builder()
                .communityId("COMM-001").fullName("Sunita Devi").staffCode("STF-001")
                .phoneNumber("9876543210").primaryRole(StaffRole.MAID)
                .gatePassNumber("GP-2024-0101").preferredLanguage("hi")
                .aadhaarLastFour("4523").photoUrl("/staff/sunita.jpg")
                .isActive(true).joinedDate(LocalDate.of(2023, 3, 15))
                .build());

        // Staff 2: Raju — Cook working at 2 flats
        DomesticStaffProfile raju = staffRepo.save(DomesticStaffProfile.builder()
                .communityId("COMM-001").fullName("Raju Kumar").staffCode("STF-002")
                .phoneNumber("9876543211").primaryRole(StaffRole.COOK)
                .gatePassNumber("GP-2024-0102").preferredLanguage("hi")
                .aadhaarLastFour("7891").photoUrl("/staff/raju.jpg")
                .isActive(true).joinedDate(LocalDate.of(2023, 6, 1))
                .build());

        // Staff 3: Meena — Nanny working at 3 flats
        DomesticStaffProfile meena = staffRepo.save(DomesticStaffProfile.builder()
                .communityId("COMM-001").fullName("Meena Kumari").staffCode("STF-003")
                .phoneNumber("9876543212").primaryRole(StaffRole.NANNY)
                .gatePassNumber("GP-2024-0103").preferredLanguage("en")
                .aadhaarLastFour("3456").photoUrl("/staff/meena.jpg")
                .isActive(true).joinedDate(LocalDate.of(2024, 1, 10))
                .build());

        // Contracts — Sunita at 3 flats with different cycles
        EmployerContract c1 = contractRepo.save(EmployerContract.builder()
                .communityId("COMM-001").staffId(sunita.getId())
                .employerFlatNumber("A-101").employerTowerName("Tower A")
                .employerName("Sharma Family").monthlySalary(5000).dailyRate(167)
                .salaryCycle(SalaryCycleType.FIRST_TO_FIRST)
                .halfDayRule(HalfDayRule.HALF_SALARY)
                .paidLeavesPerMonth(2).weeklyOffSunday(true)
                .status(ContractStatus.ACTIVE).startDate(LocalDate.of(2023, 4, 1))
                .build());

        EmployerContract c2 = contractRepo.save(EmployerContract.builder()
                .communityId("COMM-001").staffId(sunita.getId())
                .employerFlatNumber("B-204").employerTowerName("Tower B")
                .employerName("Gupta Family").monthlySalary(6000).dailyRate(200)
                .salaryCycle(SalaryCycleType.TENTH_TO_TENTH)
                .halfDayRule(HalfDayRule.NO_DEDUCTION)
                .paidLeavesPerMonth(3).weeklyOffSunday(true)
                .status(ContractStatus.ACTIVE).startDate(LocalDate.of(2023, 5, 10))
                .build());

        EmployerContract c3 = contractRepo.save(EmployerContract.builder()
                .communityId("COMM-001").staffId(sunita.getId())
                .employerFlatNumber("C-302").employerTowerName("Tower C")
                .employerName("Patel Family").monthlySalary(4500).dailyRate(150)
                .salaryCycle(SalaryCycleType.FIFTEENTH_TO_FIFTEENTH)
                .halfDayRule(HalfDayRule.FULL_DEDUCTION)
                .paidLeavesPerMonth(2).weeklyOffSunday(false)
                .status(ContractStatus.ACTIVE).startDate(LocalDate.of(2023, 7, 15))
                .build());

        // Contracts — Raju at 2 flats
        EmployerContract c4 = contractRepo.save(EmployerContract.builder()
                .communityId("COMM-001").staffId(raju.getId())
                .employerFlatNumber("A-502").employerTowerName("Tower A")
                .employerName("Mehta Family").monthlySalary(8000).dailyRate(267)
                .salaryCycle(SalaryCycleType.FIRST_TO_FIRST)
                .halfDayRule(HalfDayRule.HALF_SALARY)
                .paidLeavesPerMonth(4).weeklyOffSunday(true)
                .status(ContractStatus.ACTIVE).startDate(LocalDate.of(2023, 7, 1))
                .build());

        EmployerContract c5 = contractRepo.save(EmployerContract.builder()
                .communityId("COMM-001").staffId(raju.getId())
                .employerFlatNumber("D-101").employerTowerName("Tower D")
                .employerName("Singh Family").monthlySalary(7500).dailyRate(250)
                .salaryCycle(SalaryCycleType.TENTH_TO_TENTH)
                .halfDayRule(HalfDayRule.NO_DEDUCTION)
                .paidLeavesPerMonth(3).weeklyOffSunday(true)
                .status(ContractStatus.ACTIVE).startDate(LocalDate.of(2023, 8, 10))
                .build());

        // Contracts — Meena at 3 flats
        EmployerContract c6 = contractRepo.save(EmployerContract.builder()
                .communityId("COMM-001").staffId(meena.getId())
                .employerFlatNumber("A-101").employerTowerName("Tower A")
                .employerName("Sharma Family").monthlySalary(10000).dailyRate(333)
                .salaryCycle(SalaryCycleType.FIRST_TO_FIRST)
                .halfDayRule(HalfDayRule.HALF_SALARY)
                .paidLeavesPerMonth(4).weeklyOffSunday(true)
                .status(ContractStatus.ACTIVE).startDate(LocalDate.of(2024, 2, 1))
                .build());

        EmployerContract c7 = contractRepo.save(EmployerContract.builder()
                .communityId("COMM-001").staffId(meena.getId())
                .employerFlatNumber("B-204").employerTowerName("Tower B")
                .employerName("Gupta Family").monthlySalary(9000).dailyRate(300)
                .salaryCycle(SalaryCycleType.FIFTEENTH_TO_FIFTEENTH)
                .halfDayRule(HalfDayRule.NO_DEDUCTION)
                .paidLeavesPerMonth(3).weeklyOffSunday(true)
                .status(ContractStatus.ACTIVE).startDate(LocalDate.of(2024, 2, 15))
                .build());

        EmployerContract c8 = contractRepo.save(EmployerContract.builder()
                .communityId("COMM-001").staffId(meena.getId())
                .employerFlatNumber("E-401").employerTowerName("Tower E")
                .employerName("Reddy Family").monthlySalary(8500).dailyRate(283)
                .salaryCycle(SalaryCycleType.TENTH_TO_TENTH)
                .halfDayRule(HalfDayRule.FULL_DEDUCTION)
                .paidLeavesPerMonth(2).weeklyOffSunday(false)
                .status(ContractStatus.ACTIVE).startDate(LocalDate.of(2024, 3, 10))
                .build());

        // Attendance — Sunita: 25 days of mixed attendance across November 2026
        LocalDate month = LocalDate.of(2026, 11, 1);
        List<AttendancePunch> punches = new ArrayList<>();
        for (int d = 1; d <= 27; d++) {
            LocalDate date = month.plusDays(d - 1);
            if (date.getDayOfWeek().getValue() == 7) continue; // skip sundays

            AttendanceStatus status;
            if (d == 5 || d == 18) status = AttendanceStatus.HALF_DAY;
            else if (d == 12 || d == 25) status = AttendanceStatus.PAID_LEAVE;
            else if (d == 20) status = AttendanceStatus.ABSENT;
            else status = AttendanceStatus.PRESENT;

            punches.add(AttendancePunch.builder()
                    .communityId("COMM-001").staffId(sunita.getId())
                    .contractId(c1.getId()).employerFlatNumber("A-101")
                    .punchDate(date).punchTime(LocalTime.of(7, 30))
                    .punchType(PunchType.GATE_IN).attendanceStatus(status)
                    .autoMarked(true).durationMinutes(status == AttendanceStatus.HALF_DAY ? 120 : 240)
                    .build());
        }

        // Attendance — Raju: steady worker, almost no absences
        for (int d = 1; d <= 27; d++) {
            LocalDate date = month.plusDays(d - 1);
            if (date.getDayOfWeek().getValue() == 7) continue;

            AttendanceStatus status = (d == 15) ? AttendanceStatus.PAID_LEAVE : AttendanceStatus.PRESENT;

            punches.add(AttendancePunch.builder()
                    .communityId("COMM-001").staffId(raju.getId())
                    .contractId(c4.getId()).employerFlatNumber("A-502")
                    .punchDate(date).punchTime(LocalTime.of(10, 0))
                    .punchType(PunchType.GATE_IN).attendanceStatus(status)
                    .autoMarked(true).durationMinutes(180)
                    .build());
        }

        // Attendance — Meena
        for (int d = 1; d <= 27; d++) {
            LocalDate date = month.plusDays(d - 1);
            if (date.getDayOfWeek().getValue() == 7) continue;

            AttendanceStatus status;
            if (d == 3 || d == 17) status = AttendanceStatus.HALF_DAY;
            else if (d == 10) status = AttendanceStatus.ABSENT;
            else status = AttendanceStatus.PRESENT;

            punches.add(AttendancePunch.builder()
                    .communityId("COMM-001").staffId(meena.getId())
                    .contractId(c6.getId()).employerFlatNumber("A-101")
                    .punchDate(date).punchTime(LocalTime.of(8, 0))
                    .punchType(PunchType.GATE_IN).attendanceStatus(status)
                    .autoMarked(true).durationMinutes(status == AttendanceStatus.HALF_DAY ? 150 : 300)
                    .build());
        }

        punchRepo.saveAll(punches);

        // Salary Advance — Sunita: ₹10,000 across 4 EMIs from Sharma Family
        SalaryAdvance adv1 = advanceRepo.save(SalaryAdvance.builder()
                .communityId("COMM-001").staffId(sunita.getId()).staffName("Sunita Devi")
                .contractId(c1.getId()).employerFlatNumber("A-101").employerName("Sharma Family")
                .principalAmount(10000).totalInstallments(4).emiAmount(2500)
                .installmentsPaid(2).amountRepaid(5000).outstandingBalance(5000)
                .status(AdvanceStatus.ACTIVE).paymentMethod(PaymentMethod.UPI)
                .upiTransactionId("UPI-2026-SEP-001").disbursedAt(LocalDateTime.of(2026, 9, 1, 10, 0))
                .firstEmiDate(LocalDate.of(2026, 10, 1)).lastEmiDate(LocalDate.of(2027, 1, 1))
                .purpose("Children school fees").receiptUrlEn("/receipts/en/adv-001.pdf")
                .receiptUrlHi("/receipts/hi/adv-001.pdf")
                .build());

        // Installments for Sunita's advance
        installmentRepo.saveAll(List.of(
                AdvanceInstallment.builder().advanceId(adv1.getId()).staffId(sunita.getId()).contractId(c1.getId())
                        .installmentNumber(1).amount(2500).dueDate(LocalDate.of(2026, 10, 1))
                        .payrollMonth("2026-10").status(AdvanceInstallment.InstallmentStatus.DEDUCTED)
                        .deductedAt(LocalDateTime.of(2026, 10, 1, 12, 0)).balanceAfterDeduction(7500).build(),
                AdvanceInstallment.builder().advanceId(adv1.getId()).staffId(sunita.getId()).contractId(c1.getId())
                        .installmentNumber(2).amount(2500).dueDate(LocalDate.of(2026, 11, 1))
                        .payrollMonth("2026-11").status(AdvanceInstallment.InstallmentStatus.DEDUCTED)
                        .deductedAt(LocalDateTime.of(2026, 11, 1, 12, 0)).balanceAfterDeduction(5000).build(),
                AdvanceInstallment.builder().advanceId(adv1.getId()).staffId(sunita.getId()).contractId(c1.getId())
                        .installmentNumber(3).amount(2500).dueDate(LocalDate.of(2026, 12, 1))
                        .payrollMonth("2026-12").status(AdvanceInstallment.InstallmentStatus.SCHEDULED)
                        .balanceAfterDeduction(2500).build(),
                AdvanceInstallment.builder().advanceId(adv1.getId()).staffId(sunita.getId()).contractId(c1.getId())
                        .installmentNumber(4).amount(2500).dueDate(LocalDate.of(2027, 1, 1))
                        .payrollMonth("2027-01").status(AdvanceInstallment.InstallmentStatus.SCHEDULED)
                        .balanceAfterDeduction(0).build()
        ));

        // Salary Advance — Raju: ₹15,000 across 3 EMIs, terminated early
        SalaryAdvance adv2 = advanceRepo.save(SalaryAdvance.builder()
                .communityId("COMM-001").staffId(raju.getId()).staffName("Raju Kumar")
                .contractId(c4.getId()).employerFlatNumber("A-502").employerName("Mehta Family")
                .principalAmount(15000).totalInstallments(3).emiAmount(5000)
                .installmentsPaid(1).amountRepaid(5000).outstandingBalance(10000)
                .status(AdvanceStatus.TERMINATED_PENDING).paymentMethod(PaymentMethod.CASH)
                .disbursedAt(LocalDateTime.of(2026, 10, 1, 10, 0))
                .firstEmiDate(LocalDate.of(2026, 11, 1)).lastEmiDate(LocalDate.of(2027, 1, 1))
                .terminatedEarly(true).terminationDate(LocalDate.of(2026, 11, 15))
                .settlementAmount(10000).purpose("Medical emergency")
                .receiptUrlEn("/receipts/en/adv-002.pdf").receiptUrlHi("/receipts/hi/adv-002.pdf")
                .build());

        installmentRepo.saveAll(List.of(
                AdvanceInstallment.builder().advanceId(adv2.getId()).staffId(raju.getId()).contractId(c4.getId())
                        .installmentNumber(1).amount(5000).dueDate(LocalDate.of(2026, 11, 1))
                        .payrollMonth("2026-11").status(AdvanceInstallment.InstallmentStatus.DEDUCTED)
                        .deductedAt(LocalDateTime.of(2026, 11, 1, 12, 0)).balanceAfterDeduction(10000).build(),
                AdvanceInstallment.builder().advanceId(adv2.getId()).staffId(raju.getId()).contractId(c4.getId())
                        .installmentNumber(2).amount(5000).dueDate(LocalDate.of(2026, 12, 1))
                        .payrollMonth("2026-12").status(AdvanceInstallment.InstallmentStatus.SKIPPED)
                        .remarks("Contract terminated — settlement pending").balanceAfterDeduction(5000).build(),
                AdvanceInstallment.builder().advanceId(adv2.getId()).staffId(raju.getId()).contractId(c4.getId())
                        .installmentNumber(3).amount(5000).dueDate(LocalDate.of(2027, 1, 1))
                        .payrollMonth("2027-01").status(AdvanceInstallment.InstallmentStatus.SKIPPED)
                        .remarks("Contract terminated — settlement pending").balanceAfterDeduction(0).build()
        ));

        // Payroll ledgers — pre-computed for November 2026
        payrollRepo.saveAll(List.of(
                PayrollLedger.builder().communityId("COMM-001").staffId(sunita.getId()).staffName("Sunita Devi")
                        .contractId(c1.getId()).employerFlatNumber("A-101").employerName("Sharma Family")
                        .payrollMonth("2026-11").cycleStartDate(LocalDate.of(2026, 11, 1))
                        .cycleEndDate(LocalDate.of(2026, 11, 30)).totalDaysInCycle(30)
                        .daysPresent(20).daysAbsent(1).halfDays(2).paidLeaves(2).weeklyOffs(4).unpaidLeaves(1)
                        .grossSalary(5000).attendanceDeduction(334).advanceDeduction(2500).bonusAmount(0)
                        .netPayable(2166).status(PayrollLedger.PayrollStatus.CALCULATED).build(),
                PayrollLedger.builder().communityId("COMM-001").staffId(sunita.getId()).staffName("Sunita Devi")
                        .contractId(c2.getId()).employerFlatNumber("B-204").employerName("Gupta Family")
                        .payrollMonth("2026-11").cycleStartDate(LocalDate.of(2026, 10, 10))
                        .cycleEndDate(LocalDate.of(2026, 11, 9)).totalDaysInCycle(31)
                        .daysPresent(22).daysAbsent(0).halfDays(1).paidLeaves(3).weeklyOffs(4).unpaidLeaves(0)
                        .grossSalary(6000).attendanceDeduction(0).advanceDeduction(0).bonusAmount(0)
                        .netPayable(6000).status(PayrollLedger.PayrollStatus.PAID).build(),
                PayrollLedger.builder().communityId("COMM-001").staffId(sunita.getId()).staffName("Sunita Devi")
                        .contractId(c3.getId()).employerFlatNumber("C-302").employerName("Patel Family")
                        .payrollMonth("2026-11").cycleStartDate(LocalDate.of(2026, 10, 15))
                        .cycleEndDate(LocalDate.of(2026, 11, 14)).totalDaysInCycle(31)
                        .daysPresent(23).daysAbsent(2).halfDays(1).paidLeaves(2).weeklyOffs(0).unpaidLeaves(2)
                        .grossSalary(4500).attendanceDeduction(450).advanceDeduction(0).bonusAmount(0)
                        .netPayable(4050).status(PayrollLedger.PayrollStatus.CALCULATED).build(),
                PayrollLedger.builder().communityId("COMM-001").staffId(raju.getId()).staffName("Raju Kumar")
                        .contractId(c4.getId()).employerFlatNumber("A-502").employerName("Mehta Family")
                        .payrollMonth("2026-11").cycleStartDate(LocalDate.of(2026, 11, 1))
                        .cycleEndDate(LocalDate.of(2026, 11, 30)).totalDaysInCycle(30)
                        .daysPresent(24).daysAbsent(0).halfDays(0).paidLeaves(1).weeklyOffs(4).unpaidLeaves(0)
                        .grossSalary(8000).attendanceDeduction(0).advanceDeduction(5000).bonusAmount(0)
                        .netPayable(3000).status(PayrollLedger.PayrollStatus.CALCULATED).build(),
                PayrollLedger.builder().communityId("COMM-001").staffId(meena.getId()).staffName("Meena Kumari")
                        .contractId(c6.getId()).employerFlatNumber("A-101").employerName("Sharma Family")
                        .payrollMonth("2026-11").cycleStartDate(LocalDate.of(2026, 11, 1))
                        .cycleEndDate(LocalDate.of(2026, 11, 30)).totalDaysInCycle(30)
                        .daysPresent(21).daysAbsent(1).halfDays(2).paidLeaves(2).weeklyOffs(4).unpaidLeaves(0)
                        .grossSalary(10000).attendanceDeduction(333).advanceDeduction(0).bonusAmount(500)
                        .netPayable(10167).status(PayrollLedger.PayrollStatus.APPROVED).build()
        ));

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Demo data seeded successfully");
        result.put("staff", List.of(sunita, raju, meena));
        result.put("contracts", List.of(c1, c2, c3, c4, c5, c6, c7, c8));
        result.put("attendancePunches", punches.size());
        result.put("advances", List.of(adv1, adv2));
        result.put("payrollLedgers", 5);
        return ResponseEntity.ok(result);
    }
}
