package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.HrDepartment;
import com.manacommunity.api.hrm.entity.HrEmployee;
import com.manacommunity.api.hrm.enums.EmploymentStatus;
import com.manacommunity.api.hrm.enums.LeaveApprovalStatus;
import com.manacommunity.api.hrm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HrAnalyticsService {

    private final HrEmployeeRepository employeeRepository;
    private final HrDepartmentRepository departmentRepository;
    private final HrAttendanceRepository attendanceRepository;
    private final HrLeaveRequestRepository leaveRequestRepository;

    public Map<String, Object> getHrDashboard(Long orgId) {
        long total = employeeRepository.countByOrganizationId(orgId);
        long active = employeeRepository.countByOrganizationIdAndEmploymentStatus(orgId, EmploymentStatus.ACTIVE);
        long inactive = total - active;

        List<HrEmployee> activeEmployees = employeeRepository.findByOrganizationIdAndEmploymentStatus(orgId, EmploymentStatus.ACTIVE);
        double avgTenureMonths = activeEmployees.stream()
                .filter(e -> e.getJoiningDate() != null)
                .mapToLong(e -> ChronoUnit.MONTHS.between(e.getJoiningDate(), LocalDate.now()))
                .average()
                .orElse(0.0);

        List<HrDepartment> departments = departmentRepository.findByOrganizationId(orgId);
        Map<String, Long> deptBreakdown = new LinkedHashMap<>();
        for (HrDepartment dept : departments) {
            long count = activeEmployees.stream()
                    .filter(e -> e.getDepartment() != null && e.getDepartment().getId().equals(dept.getId()))
                    .count();
            if (count > 0) deptBreakdown.put(dept.getName(), count);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalEmployees", total);
        result.put("activeEmployees", active);
        result.put("inactiveEmployees", inactive);
        result.put("avgTenureMonths", Math.round(avgTenureMonths * 10.0) / 10.0);
        result.put("departmentBreakdown", deptBreakdown);
        return result;
    }

    public List<Map<String, Object>> getAttritionTrend(Long orgId, int months) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate now = LocalDate.now();
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthStart = now.minusMonths(i).withDayOfMonth(1);
            LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
            long exits = employeeRepository.findByOrganizationIdAndEmploymentStatus(orgId, EmploymentStatus.INACTIVE)
                    .stream()
                    .filter(e -> e.getExitDate() != null && !e.getExitDate().isBefore(monthStart) && !e.getExitDate().isAfter(monthEnd))
                    .count();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", monthStart.getMonthValue());
            entry.put("year", monthStart.getYear());
            entry.put("label", monthStart.getMonth().name().substring(0, 3) + " " + monthStart.getYear());
            entry.put("exits", exits);
            trend.add(entry);
        }
        return trend;
    }

    public List<Map<String, Object>> getHeadcountTrend(Long orgId, int months) {
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate now = LocalDate.now();
        List<HrEmployee> allEmployees = employeeRepository.findByOrganizationIdAndEmploymentStatus(orgId, EmploymentStatus.ACTIVE);
        for (int i = months - 1; i >= 0; i--) {
            LocalDate monthEnd = now.minusMonths(i).withDayOfMonth(now.minusMonths(i).lengthOfMonth());
            long headcount = allEmployees.stream()
                    .filter(e -> e.getJoiningDate() != null && !e.getJoiningDate().isAfter(monthEnd))
                    .count();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", monthEnd.getMonthValue());
            entry.put("year", monthEnd.getYear());
            entry.put("label", monthEnd.getMonth().name().substring(0, 3) + " " + monthEnd.getYear());
            entry.put("headcount", headcount);
            trend.add(entry);
        }
        return trend;
    }

    public List<Map<String, Object>> getDepartmentBreakdown(Long orgId) {
        List<HrEmployee> activeEmployees = employeeRepository.findByOrganizationIdAndEmploymentStatus(orgId, EmploymentStatus.ACTIVE);
        List<HrDepartment> departments = departmentRepository.findByOrganizationId(orgId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (HrDepartment dept : departments) {
            List<HrEmployee> deptEmps = activeEmployees.stream()
                    .filter(e -> e.getDepartment() != null && e.getDepartment().getId().equals(dept.getId()))
                    .collect(Collectors.toList());
            if (!deptEmps.isEmpty()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("departmentId", dept.getId());
                row.put("departmentName", dept.getName());
                row.put("employeeCount", deptEmps.size());
                result.add(row);
            }
        }
        return result;
    }

    public Map<String, Object> getAttendanceAnalytics(Long orgId, int year, int month) {
        LocalDate monthStart = LocalDate.of(year, month, 1);
        LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());
        int workingDays = 0;
        for (LocalDate d = monthStart; !d.isAfter(monthEnd); d = d.plusDays(1)) {
            int dow = d.getDayOfWeek().getValue();
            if (dow < 6) workingDays++;
        }
        long totalEmployees = employeeRepository.countByOrganizationIdAndEmploymentStatus(orgId, EmploymentStatus.ACTIVE);
        long expectedRecords = totalEmployees * workingDays;
        // Count PRESENT records
        long presentCount = 0;
        for (LocalDate d = monthStart; !d.isAfter(monthEnd); d = d.plusDays(1)) {
            int dow = d.getDayOfWeek().getValue();
            if (dow < 6) {
                presentCount += attendanceRepository.countByOrganizationIdAndAttendanceDateAndAttendanceStatus(
                        orgId, d, com.manacommunity.api.hrm.enums.AttendanceStatus.PRESENT);
            }
        }
        double attendanceRate = expectedRecords > 0 ? (presentCount * 100.0 / expectedRecords) : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("workingDays", workingDays);
        result.put("totalEmployees", totalEmployees);
        result.put("totalPresentRecords", presentCount);
        result.put("attendanceRatePercent", Math.round(attendanceRate * 10.0) / 10.0);
        return result;
    }

    public Map<String, Object> getLeaveAnalytics(Long orgId, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        long approvedLeaves = leaveRequestRepository.countByOrganizationIdAndApprovalStatus(orgId, LeaveApprovalStatus.APPROVED);
        long pendingLeaves = leaveRequestRepository.countByOrganizationIdAndApprovalStatus(orgId, LeaveApprovalStatus.PENDING);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("approvedLeaveRequests", approvedLeaves);
        result.put("pendingLeaveRequests", pendingLeaves);
        return result;
    }

    public Map<String, Object> getWorkforceMetrics(Long orgId) {
        List<HrEmployee> activeEmployees = employeeRepository.findByOrganizationIdAndEmploymentStatus(orgId, EmploymentStatus.ACTIVE);
        LocalDate today = LocalDate.now();

        // Tenure distribution bands
        Map<String, Long> tenureBands = new LinkedHashMap<>();
        tenureBands.put("less_than_1_year", 0L);
        tenureBands.put("1_to_3_years", 0L);
        tenureBands.put("3_to_5_years", 0L);
        tenureBands.put("5_to_10_years", 0L);
        tenureBands.put("over_10_years", 0L);

        for (HrEmployee emp : activeEmployees) {
            if (emp.getJoiningDate() != null) {
                long months = ChronoUnit.MONTHS.between(emp.getJoiningDate(), today);
                if (months < 12) tenureBands.merge("less_than_1_year", 1L, Long::sum);
                else if (months < 36) tenureBands.merge("1_to_3_years", 1L, Long::sum);
                else if (months < 60) tenureBands.merge("3_to_5_years", 1L, Long::sum);
                else if (months < 120) tenureBands.merge("5_to_10_years", 1L, Long::sum);
                else tenureBands.merge("over_10_years", 1L, Long::sum);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalActive", activeEmployees.size());
        result.put("tenureBands", tenureBands);
        return result;
    }
}
