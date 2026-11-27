package com.manacommunity.api.staffpayroll.service;

import com.manacommunity.api.staffpayroll.entity.*;
import com.manacommunity.api.staffpayroll.entity.AttendancePunch.AttendanceStatus;
import com.manacommunity.api.staffpayroll.entity.EmployerContract.*;
import com.manacommunity.api.staffpayroll.entity.PayrollLedger.PayrollStatus;
import com.manacommunity.api.staffpayroll.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProRataWageEngine {

    private final DomesticStaffProfileRepository staffRepo;
    private final EmployerContractRepository contractRepo;
    private final AttendancePunchRepository punchRepo;
    private final PayrollLedgerRepository payrollRepo;
    private final SalaryAdvanceRepository advanceRepo;

    @Transactional
    public List<PayrollLedger> calculateMonthlyPayroll(Long staffId, String payrollMonth) {
        List<EmployerContract> contracts = contractRepo.findActiveContracts(staffId);
        DomesticStaffProfile staff = staffRepo.findById(staffId)
                .orElseThrow(() -> new RuntimeException("Staff not found"));

        List<PayrollLedger> ledgers = new ArrayList<>();

        for (EmployerContract contract : contracts) {
            LocalDate cycleStart = getCycleStart(contract, payrollMonth);
            LocalDate cycleEnd = getCycleEnd(contract, payrollMonth);

            List<AttendancePunch> punches = punchRepo.findByStaffIdAndPunchDateBetween(staffId, cycleStart, cycleEnd);
            Map<LocalDate, AttendanceStatus> dailyStatus = aggregateAttendance(punches, contract, cycleStart, cycleEnd);

            int totalDays = (int) ChronoUnit.DAYS.between(cycleStart, cycleEnd) + 1;
            int present = 0, halfDays = 0, paidLeaves = 0, weeklyOffs = 0, absent = 0;

            for (Map.Entry<LocalDate, AttendanceStatus> entry : dailyStatus.entrySet()) {
                switch (entry.getValue()) {
                    case PRESENT -> present++;
                    case HALF_DAY -> halfDays++;
                    case PAID_LEAVE -> paidLeaves++;
                    case WEEKLY_OFF -> weeklyOffs++;
                    case ABSENT -> absent++;
                }
            }

            int unpaidLeaves = Math.max(0, absent - 0);
            double dailyRate = contract.getDailyRate() > 0 ? contract.getDailyRate() : contract.getMonthlySalary() / totalDays;

            double halfDayDeduction = switch (contract.getHalfDayRule()) {
                case HALF_SALARY -> halfDays * dailyRate * 0.5;
                case FULL_DEDUCTION -> halfDays * dailyRate;
                case NO_DEDUCTION -> 0;
            };

            double attendanceDeduction = (unpaidLeaves * dailyRate) + halfDayDeduction;
            double grossSalary = contract.getMonthlySalary();

            double advanceDeduction = 0;
            List<SalaryAdvance> activeAdvances = advanceRepo.findActiveAdvances(contract.getId());
            for (SalaryAdvance adv : activeAdvances) {
                advanceDeduction += adv.getEmiAmount();
            }

            double netPayable = grossSalary - attendanceDeduction - advanceDeduction;

            PayrollLedger ledger = PayrollLedger.builder()
                    .communityId(contract.getCommunityId())
                    .staffId(staffId).staffName(staff.getFullName())
                    .contractId(contract.getId())
                    .employerFlatNumber(contract.getEmployerFlatNumber())
                    .employerName(contract.getEmployerName())
                    .payrollMonth(payrollMonth)
                    .cycleStartDate(cycleStart).cycleEndDate(cycleEnd)
                    .totalDaysInCycle(totalDays)
                    .daysPresent(present).daysAbsent(absent)
                    .halfDays(halfDays).paidLeaves(paidLeaves)
                    .weeklyOffs(weeklyOffs).unpaidLeaves(unpaidLeaves)
                    .grossSalary(grossSalary)
                    .attendanceDeduction(Math.round(attendanceDeduction * 100.0) / 100.0)
                    .advanceDeduction(advanceDeduction)
                    .bonusAmount(0)
                    .netPayable(Math.round(netPayable * 100.0) / 100.0)
                    .status(PayrollStatus.CALCULATED)
                    .build();

            ledgers.add(payrollRepo.save(ledger));
        }

        return ledgers;
    }

    private LocalDate getCycleStart(EmployerContract contract, String month) {
        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int mon = Integer.parseInt(parts[1]);

        return switch (contract.getSalaryCycle()) {
            case TENTH_TO_TENTH -> LocalDate.of(mon == 1 ? year - 1 : year, mon == 1 ? 12 : mon - 1, 10);
            case FIFTEENTH_TO_FIFTEENTH -> LocalDate.of(mon == 1 ? year - 1 : year, mon == 1 ? 12 : mon - 1, 15);
            default -> LocalDate.of(year, mon, 1);
        };
    }

    private LocalDate getCycleEnd(EmployerContract contract, String month) {
        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int mon = Integer.parseInt(parts[1]);

        return switch (contract.getSalaryCycle()) {
            case TENTH_TO_TENTH -> LocalDate.of(year, mon, 9);
            case FIFTEENTH_TO_FIFTEENTH -> LocalDate.of(year, mon, 14);
            default -> YearMonth.of(year, mon).atEndOfMonth();
        };
    }

    private Map<LocalDate, AttendanceStatus> aggregateAttendance(List<AttendancePunch> punches,
                                                                    EmployerContract contract,
                                                                    LocalDate start, LocalDate end) {
        Map<LocalDate, AttendanceStatus> result = new LinkedHashMap<>();
        Set<LocalDate> punchDates = punches.stream()
                .filter(p -> p.getAttendanceStatus() != null)
                .collect(Collectors.toMap(AttendancePunch::getPunchDate, AttendancePunch::getAttendanceStatus, (a, b) -> a))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        int paidLeavesUsed = 0;

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            if (contract.isWeeklyOffSunday() && d.getDayOfWeek() == DayOfWeek.SUNDAY) {
                result.put(d, AttendanceStatus.WEEKLY_OFF);
            } else if (punchDates.containsKey(d)) {
                result.put(d, punchDates.get(d));
                if (punchDates.get(d) == AttendanceStatus.PAID_LEAVE) paidLeavesUsed++;
            } else {
                if (paidLeavesUsed < contract.getPaidLeavesPerMonth()) {
                    result.put(d, AttendanceStatus.PAID_LEAVE);
                    paidLeavesUsed++;
                } else {
                    result.put(d, AttendanceStatus.ABSENT);
                }
            }
        }

        return result;
    }

    public StaffPayrollDashboard getDashboard() {
        List<DomesticStaffProfile> allStaff = staffRepo.findAll();
        List<EmployerContract> allContracts = contractRepo.findAll();
        List<PayrollLedger> allPayrolls = payrollRepo.findAll();
        List<SalaryAdvance> allAdvances = advanceRepo.findAll();

        double totalMonthlyWages = allContracts.stream()
                .filter(c -> c.getStatus() == ContractStatus.ACTIVE)
                .mapToDouble(EmployerContract::getMonthlySalary).sum();

        double totalPaid = allPayrolls.stream()
                .filter(p -> p.getStatus() == PayrollStatus.PAID)
                .mapToDouble(PayrollLedger::getNetPayable).sum();

        double totalPending = allPayrolls.stream()
                .filter(p -> p.getStatus() == PayrollStatus.CALCULATED)
                .mapToDouble(PayrollLedger::getNetPayable).sum();

        double totalAdvancesOutstanding = allAdvances.stream()
                .filter(a -> a.getStatus() == SalaryAdvance.AdvanceStatus.ACTIVE)
                .mapToDouble(SalaryAdvance::getOutstandingBalance).sum();

        Map<String, Long> staffByRole = allStaff.stream()
                .collect(Collectors.groupingBy(s -> s.getPrimaryRole().name(), Collectors.counting()));

        Map<String, Long> contractsByCycle = allContracts.stream()
                .filter(c -> c.getStatus() == ContractStatus.ACTIVE)
                .collect(Collectors.groupingBy(c -> c.getSalaryCycle().name(), Collectors.counting()));

        // Per-staff summary
        List<StaffSummary> staffSummaries = allStaff.stream().map(staff -> {
            List<EmployerContract> contracts = allContracts.stream()
                    .filter(c -> c.getStaffId().equals(staff.getId()) && c.getStatus() == ContractStatus.ACTIVE)
                    .toList();
            double totalSalary = contracts.stream().mapToDouble(EmployerContract::getMonthlySalary).sum();
            double outstanding = allAdvances.stream()
                    .filter(a -> a.getStaffId().equals(staff.getId()) && a.getStatus() == SalaryAdvance.AdvanceStatus.ACTIVE)
                    .mapToDouble(SalaryAdvance::getOutstandingBalance).sum();

            return new StaffSummary(staff.getId(), staff.getFullName(), staff.getStaffCode(),
                    staff.getPrimaryRole().name(), contracts.size(), totalSalary, outstanding);
        }).toList();

        return new StaffPayrollDashboard(allStaff.size(), allContracts.stream().filter(c -> c.getStatus() == ContractStatus.ACTIVE).count(),
                totalMonthlyWages, totalPaid, totalPending, totalAdvancesOutstanding,
                staffByRole, contractsByCycle, staffSummaries,
                allPayrolls, allAdvances, allContracts);
    }

    public record StaffSummary(Long staffId, String name, String staffCode, String role,
                                 int employers, double totalMonthlySalary, double advanceOutstanding) {}

    public record StaffPayrollDashboard(int totalStaff, long activeContracts,
                                          double totalMonthlyWages, double totalPaid, double totalPending,
                                          double totalAdvancesOutstanding,
                                          Map<String, Long> staffByRole, Map<String, Long> contractsByCycle,
                                          List<StaffSummary> staffSummaries,
                                          List<PayrollLedger> payrolls, List<SalaryAdvance> advances,
                                          List<EmployerContract> contracts) {}
}
