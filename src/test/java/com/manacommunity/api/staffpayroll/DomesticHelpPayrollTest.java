package com.manacommunity.api.staffpayroll;

import com.manacommunity.api.staffpayroll.entity.AdvanceInstallment;
import com.manacommunity.api.staffpayroll.entity.AdvanceInstallment.InstallmentStatus;
import com.manacommunity.api.staffpayroll.entity.AttendancePunch;
import com.manacommunity.api.staffpayroll.entity.AttendancePunch.AttendanceStatus;
import com.manacommunity.api.staffpayroll.entity.DomesticStaffProfile;
import com.manacommunity.api.staffpayroll.entity.DomesticStaffProfile.StaffRole;
import com.manacommunity.api.staffpayroll.entity.EmployerContract;
import com.manacommunity.api.staffpayroll.entity.EmployerContract.ContractStatus;
import com.manacommunity.api.staffpayroll.entity.EmployerContract.HalfDayRule;
import com.manacommunity.api.staffpayroll.entity.EmployerContract.SalaryCycleType;
import com.manacommunity.api.staffpayroll.entity.PayrollLedger;
import com.manacommunity.api.staffpayroll.entity.SalaryAdvance;
import com.manacommunity.api.staffpayroll.entity.SalaryAdvance.AdvanceStatus;
import com.manacommunity.api.staffpayroll.entity.SalaryAdvance.PaymentMethod;
import com.manacommunity.api.staffpayroll.repository.AdvanceInstallmentRepository;
import com.manacommunity.api.staffpayroll.repository.AttendancePunchRepository;
import com.manacommunity.api.staffpayroll.repository.DomesticStaffProfileRepository;
import com.manacommunity.api.staffpayroll.repository.EmployerContractRepository;
import com.manacommunity.api.staffpayroll.repository.PayrollLedgerRepository;
import com.manacommunity.api.staffpayroll.repository.SalaryAdvanceRepository;
import com.manacommunity.api.staffpayroll.service.AdvanceAmortizationService;
import com.manacommunity.api.staffpayroll.service.ProRataWageEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomesticHelpPayrollTest {

    @Mock private DomesticStaffProfileRepository staffRepo;
    @Mock private EmployerContractRepository contractRepo;
    @Mock private AttendancePunchRepository punchRepo;
    @Mock private PayrollLedgerRepository payrollRepo;
    @Mock private SalaryAdvanceRepository advanceRepo;
    @Mock private AdvanceInstallmentRepository installmentRepo;

    private ProRataWageEngine wageEngine;
    private AdvanceAmortizationService advanceService;

    private final List<EmployerContract> contracts = new ArrayList<>();
    private final List<AttendancePunch> punches = new ArrayList<>();
    private final List<SalaryAdvance> advances = new ArrayList<>();
    private final List<AdvanceInstallment> installments = new ArrayList<>();
    private final AtomicLong seq = new AtomicLong(1L);

    @BeforeEach
    void setUp() {
        contracts.clear();
        punches.clear();
        advances.clear();
        installments.clear();

        wageEngine = new ProRataWageEngine(staffRepo, contractRepo, punchRepo, payrollRepo, advanceRepo);
        advanceService = new AdvanceAmortizationService(advanceRepo, installmentRepo, contractRepo);
    }

    @Test
    void calculatesPayrollAcross8FlatsWithMixedCyclesLeavesHalfDaysAndAdvanceEmi() {
        DomesticStaffProfile cook = DomesticStaffProfile.builder()
                .id(10L).communityId(1L).staffCode("COOK-010").fullName("Lakshmi Devi")
                .primaryRole(StaffRole.COOK).totalEmployers(8).build();
        when(staffRepo.findById(10L)).thenReturn(Optional.of(cook));

        // Build 8 flat contracts for the cook: 4 on 1st-to-1st, 4 on 10th-to-10th
        for (int i = 1; i <= 8; i++) {
            SalaryCycleType cycle = i <= 4 ? SalaryCycleType.FIRST_TO_FIRST : SalaryCycleType.TENTH_TO_TENTH;
            HalfDayRule halfRule = (i % 2 == 0) ? HalfDayRule.HALF_SALARY : HalfDayRule.NO_DEDUCTION;
            contracts.add(EmployerContract.builder()
                    .id(100L + i)
                    .communityId(1L)
                    .staffId(10L)
                    .staffName("Lakshmi Devi")
                    .employerFlatNumber("A-" + (100 + i))
                    .employerName("Resident " + i)
                    .salaryCycle(cycle)
                    .monthlySalary(6000.0) // ₹200/day over 30 days
                    .dailyRate(200.0)
                    .paidLeavesPerMonth(2)
                    .halfDayRule(halfRule)
                    .weeklyOffSunday(true)
                    .status(ContractStatus.ACTIVE)
                    .build());
        }
        when(contractRepo.findActiveContracts(10L)).thenReturn(contracts);

        // Populate daily PRESENT punches from Oct 10 through Nov 30, with 1 HALF_DAY on Nov 12
        // and 3 unpunched weekdays in Nov (first 2 covered by paidLeavesPerMonth=2, 3rd unpaid)
        LocalDate d = LocalDate.of(2026, 10, 10);
        LocalDate end = LocalDate.of(2026, 11, 30);
        while (!d.isAfter(end)) {
            if (d.getDayOfWeek() != DayOfWeek.SUNDAY
                    && !d.equals(LocalDate.of(2026, 11, 3))
                    && !d.equals(LocalDate.of(2026, 11, 4))
                    && !d.equals(LocalDate.of(2026, 11, 5))) {
                AttendanceStatus st = d.equals(LocalDate.of(2026, 11, 12))
                        ? AttendanceStatus.HALF_DAY
                        : AttendanceStatus.PRESENT;
                punches.add(AttendancePunch.builder()
                        .id(seq.getAndIncrement()).communityId(1L).staffId(10L)
                        .punchDate(d).attendanceStatus(st).build());
            }
            d = d.plusDays(1);
        }

        when(punchRepo.findByStaffIdAndPunchDateBetween(eq(10L), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    LocalDate s = inv.getArgument(1);
                    LocalDate e = inv.getArgument(2);
                    return punches.stream()
                            .filter(p -> !p.getPunchDate().isBefore(s) && !p.getPunchDate().isAfter(e))
                            .toList();
                });

        // Contract #2 (FIRST_TO_FIRST, HALF_SALARY half-day rule) has active advance EMI ₹1,000/mo
        SalaryAdvance activeAdv = SalaryAdvance.builder()
                .id(501L).communityId(1L).staffId(10L).contractId(102L)
                .principalAmount(3000.0).totalInstallments(3).emiAmount(1000.0)
                .outstandingBalance(3000.0).status(AdvanceStatus.ACTIVE).build();
        when(advanceRepo.findActiveAdvances(anyLong())).thenAnswer(inv -> {
            Long cid = inv.getArgument(0);
            return cid.equals(102L) ? List.of(activeAdv) : List.of();
        });
        when(payrollRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        List<PayrollLedger> ledgers = wageEngine.calculateMonthlyPayroll(10L, "2026-11");

        assertThat(ledgers).hasSize(8);

        // Verify cycle boundaries: Flat 1 (FIRST_TO_FIRST) vs Flat 5 (TENTH_TO_TENTH)
        PayrollLedger flat1 = ledgers.get(0);
        PayrollLedger flat5 = ledgers.get(4);
        assertThat(flat1.getCycleStartDate()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(flat1.getCycleEndDate()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(flat5.getCycleStartDate()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(flat5.getCycleEndDate()).isEqualTo(LocalDate.of(2026, 11, 9));

        // Flat 2 (1st-1st): 2 paid leaves used (Nov 3,4), 1 unpaid leave (Nov 5 = ₹200),
        // 1 half-day @ HALF_SALARY (₹100) + ₹1000 EMI => net = 6000 - 300 - 1000 = ₹4700
        PayrollLedger flat2 = ledgers.get(1);
        assertThat(flat2.getPaidLeaves()).isEqualTo(2);
        assertThat(flat2.getUnpaidLeaves()).isEqualTo(1);
        assertThat(flat2.getHalfDays()).isEqualTo(1);
        assertThat(flat2.getAttendanceDeduction()).isEqualTo(300.0);
        assertThat(flat2.getAdvanceDeduction()).isEqualTo(1000.0);
        assertThat(flat2.getNetPayable()).isEqualTo(4700.0);

        // Flat 5 (10th Oct - 9th Nov): NO_DEDUCTION half-day rule, Nov 3+4 absorbed as paidLeaves=2,
        // Nov 5 unpaid=1 (₹200), no EMI => net = ₹5800
        assertThat(flat5.getPaidLeaves()).isEqualTo(2);
        assertThat(flat5.getUnpaidLeaves()).isEqualTo(1);
        assertThat(flat5.getAttendanceDeduction()).isEqualTo(200.0);
        assertThat(flat5.getAdvanceDeduction()).isZero();
        assertThat(flat5.getNetPayable()).isEqualTo(5800.0);
    }

    @Test
    void amortizesAdvanceAcrossInstallmentsAndSettlesMidMonthTermination() {
        EmployerContract contract = EmployerContract.builder()
                .id(201L).communityId(1L).staffId(10L).staffName("Lakshmi Devi")
                .employerFlatNumber("B-304").employerName("Meera")
                .salaryCycle(SalaryCycleType.TENTH_TO_TENTH).monthlySalary(5000.0)
                .status(ContractStatus.ACTIVE).build();
        when(contractRepo.findById(201L)).thenReturn(Optional.of(contract));
        when(advanceRepo.save(any())).thenAnswer(inv -> {
            SalaryAdvance a = inv.getArgument(0);
            if (a.getId() == null) a.setId(900L);
            advances.removeIf(x -> x.getId().equals(a.getId()));
            advances.add(a);
            return a;
        });
        when(advanceRepo.findById(900L)).thenAnswer(inv -> advances.stream().findFirst());
        when(installmentRepo.saveAll(any())).thenAnswer(inv -> {
            List<AdvanceInstallment> list = inv.getArgument(0);
            installments.clear();
            installments.addAll(list);
            return list;
        });
        when(installmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(installmentRepo.findByAdvanceIdOrderByInstallmentNumber(900L)).thenReturn(installments);
        when(installmentRepo.findByAdvanceIdAndStatus(900L, InstallmentStatus.SCHEDULED))
                .thenAnswer(inv -> installments.stream()
                        .filter(i -> i.getStatus() == InstallmentStatus.SCHEDULED).toList());

        // Disburse ₹3,000 over 3 EMIs (₹1,000 each)
        SalaryAdvance disbursed = advanceService.disburseAdvance(
                10L, "Lakshmi Devi", 201L, 3000.0, 3, PaymentMethod.UPI, "UPI-TXN-77", "Medical");
        assertThat(disbursed.getEmiAmount()).isEqualTo(1000.0);
        assertThat(disbursed.getFirstEmiDate().getDayOfMonth()).isEqualTo(10);
        assertThat(installments).hasSize(3);

        // Deduct EMI #1 -> balance drops to ₹2,000
        AdvanceInstallment first = advanceService.deductInstallment(900L, 1);
        assertThat(first.getStatus()).isEqualTo(InstallmentStatus.DEDUCTED);
        assertThat(advances.get(0).getOutstandingBalance()).isEqualTo(2000.0);

        // Mid-month termination -> outstanding ₹2,000 captured as settlement, remaining 2 EMIs SKIPPED
        SalaryAdvance terminated = advanceService.handleMidMonthTermination(900L);
        assertThat(terminated.getStatus()).isEqualTo(AdvanceStatus.TERMINATED_PENDING);
        assertThat(terminated.getSettlementAmount()).isEqualTo(2000.0);
        assertThat(installments.stream().filter(i -> i.getStatus() == InstallmentStatus.SKIPPED)).hasSize(2);
    }
}
