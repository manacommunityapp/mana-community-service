package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.enums.EmploymentStatus;
import com.manacommunity.api.hrm.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrPayrollService {

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

        double totalGross = 0, totalDeductions = 0, totalNet = 0;
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
            double ratio = workingDays > 0 ? (double) paidDays / workingDays : 1.0;

            double basic = structure.getBasicMonthly() * ratio;
            double hra = structure.getHraMonthly() * ratio;
            double da = structure.getDaMonthly() * ratio;
            double special = structure.getSpecialAllowanceMonthly() * ratio;
            double other = structure.getOtherAllowancesMonthly() * ratio;
            double gross = basic + hra + da + special + other;

            // India-statutory deductions — rates subject to regulatory validation before production use
            double pfEmp = Math.min(basic * 0.12, 1800);
            double pfEmr = Math.min(basic * 0.12, 1800);
            double esiEmp = gross <= 21000 ? gross * 0.0075 : 0;
            double esiEmr = gross <= 21000 ? gross * 0.0325 : 0;
            double pt = structure.getProfessionalTaxMonthly();
            double tds = structure.getTdsMonthly() * ratio;
            double totalDed = pfEmp + esiEmp + pt + tds;
            double net = gross - totalDed;

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

            totalGross += gross;
            totalDeductions += totalDed;
            totalNet += net;
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
}
