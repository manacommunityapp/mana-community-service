package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.enums.EmploymentStatus;
import com.manacommunity.api.hrm.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrPayrollService {

    private static final BigDecimal PF_RATE = new BigDecimal("0.12");
    private static final BigDecimal PF_CAP = new BigDecimal("1800.00");
    private static final BigDecimal ESI_THRESHOLD = new BigDecimal("21000.00");
    private static final BigDecimal ESI_EMP_RATE = new BigDecimal("0.0075");
    private static final BigDecimal ESI_EMR_RATE = new BigDecimal("0.0325");

    private final HrPayrollRunRepository payrollRunRepository;
    private final HrPayslipRepository payslipRepository;
    private final HrSalaryStructureRepository salaryStructureRepository;
    private final HrSalaryComponentRepository componentRepository;
    private final HrEmployeeRepository employeeRepository;
    private final HrAttendanceRepository attendanceRepository;

    @Transactional
    public HrPayrollRun createRun(Long orgId, int month, int year, Long createdBy) {
        if (payrollRunRepository.findByOrganizationIdAndPayrollMonthAndPayrollYear(orgId, month, year).isPresent()) {
            throw new IllegalStateException("Payroll run already exists for " + month + "/" + year);
        }
        return payrollRunRepository.save(HrPayrollRun.builder()
            .organizationId(orgId)
            .payrollMonth(month)
            .payrollYear(year)
            .processedBy(createdBy)
            .build());
    }

    @Transactional
    public HrPayrollRun processPayroll(Long orgId, Long runId) {
        HrPayrollRun run = payrollRunRepository.findById(runId)
            .orElseThrow(() -> new EntityNotFoundException("Payroll run not found"));
        if (!run.getOrganizationId().equals(orgId)) throw new AccessDeniedException("Access denied");
        if (!"DRAFT".equals(run.getStatus())) throw new IllegalStateException("Only DRAFT runs can be processed");

        List<HrEmployee> employees = employeeRepository.findByOrganizationIdAndEmploymentStatus(orgId, EmploymentStatus.ACTIVE);
        LocalDate periodStart = LocalDate.of(run.getPayrollYear(), run.getPayrollMonth(), 1);
        LocalDate periodEnd = periodStart.withDayOfMonth(periodStart.lengthOfMonth());
        int workingDays = (int) periodStart.datesUntil(periodEnd.plusDays(1))
            .filter(d -> d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY)
            .count();

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        for (HrEmployee emp : employees) {
            HrSalaryStructure structure = salaryStructureRepository
                .findFirstByEmployeeIdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(emp.getId(), periodEnd)
                .orElse(null);
            if (structure == null) continue;

            long attendedDays = attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(emp.getId(), periodStart, periodEnd)
                .stream().filter(a -> a.getAttendanceStatus() != null &&
                    !a.getAttendanceStatus().name().equals("ABSENT") &&
                    !a.getAttendanceStatus().name().equals("WEEKEND") &&
                    !a.getAttendanceStatus().name().equals("HOLIDAY"))
                .count();
            int paidDays = (int) Math.min(attendedDays, workingDays);
            BigDecimal ratio = workingDays > 0
                ? BigDecimal.valueOf(paidDays).divide(BigDecimal.valueOf(workingDays), 6, RoundingMode.HALF_UP)
                : BigDecimal.ONE;

            BigDecimal basic = prorate(structure.getBasicMonthly(), ratio);
            BigDecimal hra = prorate(structure.getHraMonthly(), ratio);
            BigDecimal da = prorate(structure.getDaMonthly(), ratio);
            BigDecimal special = prorate(structure.getSpecialAllowanceMonthly(), ratio);
            BigDecimal other = prorate(structure.getOtherAllowancesMonthly(), ratio);
            BigDecimal gross = basic.add(hra).add(da).add(special).add(other);

            // India-statutory deductions — rates subject to regulatory validation before production use
            BigDecimal pfEmp = basic.multiply(PF_RATE).setScale(2, RoundingMode.HALF_UP).min(PF_CAP);
            BigDecimal pfEmr = basic.multiply(PF_RATE).setScale(2, RoundingMode.HALF_UP).min(PF_CAP);
            BigDecimal esiEmp = gross.compareTo(ESI_THRESHOLD) <= 0
                ? gross.multiply(ESI_EMP_RATE).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal esiEmr = gross.compareTo(ESI_THRESHOLD) <= 0
                ? gross.multiply(ESI_EMR_RATE).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal pt = structure.getProfessionalTaxMonthly() != null
                ? structure.getProfessionalTaxMonthly().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal tds = prorate(structure.getTdsMonthly(), ratio);
            BigDecimal totalDed = pfEmp.add(esiEmp).add(pt).add(tds);
            BigDecimal net = gross.subtract(totalDed);

            HrPayslip payslip = payslipRepository.findByEmployeeIdAndPayrollRunId(emp.getId(), runId)
                .orElseGet(() -> HrPayslip.builder()
                    .organizationId(orgId).employee(emp).payrollRun(run).build());

            payslip.setWorkingDays(workingDays);
            payslip.setPaidDays(paidDays);
            payslip.setLossOfPayDays(workingDays - paidDays);
            payslip.setBasicSalary(basic);
            payslip.setHra(hra);
            payslip.setDa(da);
            payslip.setSpecialAllowance(special);
            payslip.setOtherAllowances(other);
            payslip.setGrossSalary(gross);
            payslip.setPfEmployee(pfEmp);
            payslip.setPfEmployer(pfEmr);
            payslip.setEsiEmployee(esiEmp);
            payslip.setEsiEmployer(esiEmr);
            payslip.setProfessionalTax(pt);
            payslip.setTds(tds);
            payslip.setTotalDeductions(totalDed);
            payslip.setNetSalary(net);
            payslipRepository.save(payslip);

            totalGross = totalGross.add(gross);
            totalDeductions = totalDeductions.add(totalDed);
            totalNet = totalNet.add(net);
        }

        run.setStatus("PROCESSED");
        run.setTotalGross(totalGross);
        run.setTotalDeductions(totalDeductions);
        run.setTotalNet(totalNet);
        run.setEmployeeCount(employees.size());
        run.setProcessedAt(LocalDateTime.now());
        return payrollRunRepository.save(run);
    }

    @Transactional
    public HrPayrollRun approvePayroll(Long orgId, Long runId, Long approvedBy) {
        HrPayrollRun run = payrollRunRepository.findById(runId)
            .orElseThrow(() -> new EntityNotFoundException("Payroll run not found"));
        if (!run.getOrganizationId().equals(orgId)) throw new AccessDeniedException("Access denied");
        if (!"PROCESSED".equals(run.getStatus())) throw new IllegalStateException("Only PROCESSED runs can be approved");
        run.setStatus("APPROVED");
        run.setApprovedBy(approvedBy);
        run.setApprovedAt(LocalDateTime.now());
        return payrollRunRepository.save(run);
    }

    @Transactional
    public void publishPayslips(Long orgId, Long runId) {
        HrPayrollRun run = payrollRunRepository.findById(runId)
            .orElseThrow(() -> new EntityNotFoundException("Payroll run not found"));
        if (!run.getOrganizationId().equals(orgId)) throw new AccessDeniedException("Access denied");
        if (!"APPROVED".equals(run.getStatus())) throw new IllegalStateException("Run must be APPROVED before publishing");
        LocalDateTime now = LocalDateTime.now();
        for (HrPayslip ps : payslipRepository.findByPayrollRunId(runId)) {
            ps.setIsPublished(true);
            ps.setPublishedAt(now);
            payslipRepository.save(ps);
        }
        run.setStatus("PAID");
        payrollRunRepository.save(run);
    }

    public List<HrPayrollRun> getRuns(Long orgId) {
        return payrollRunRepository.findByOrganizationIdOrderByPayrollYearDescPayrollMonthDesc(orgId);
    }

    public List<HrPayslip> getPayslips(Long runId) {
        return payslipRepository.findByPayrollRunId(runId);
    }

    public List<HrPayslip> getMyPayslips(Long employeeId) {
        return payslipRepository.findByEmployeeIdAndIsPublishedTrue(employeeId);
    }

    @Transactional
    public HrSalaryStructure setSalaryStructure(Long orgId, Long employeeId, HrSalaryStructure structure, Long createdBy) {
        HrEmployee emp = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        structure.setOrganizationId(orgId);
        structure.setEmployee(emp);
        structure.setCreatedBy(createdBy);
        return salaryStructureRepository.save(structure);
    }

    public List<HrSalaryStructure> getSalaryHistory(Long employeeId) {
        return salaryStructureRepository.findByEmployeeIdOrderByEffectiveFromDesc(employeeId);
    }

    public List<HrSalaryComponent> getComponents(Long orgId) {
        return componentRepository.findByOrganizationIdAndIsActiveTrueOrderBySequenceOrder(orgId);
    }

    @Transactional
    public HrSalaryComponent saveComponent(Long orgId, HrSalaryComponent component) {
        component.setOrganizationId(orgId);
        return componentRepository.save(component);
    }

    private BigDecimal prorate(BigDecimal amount, BigDecimal ratio) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
    }
}
