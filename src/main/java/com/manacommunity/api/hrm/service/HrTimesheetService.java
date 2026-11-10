package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrTimesheetService {

    private final HrTimesheetRepository timesheetRepository;
    private final HrAttendanceRepository attendanceRepository;
    private final HrEmployeeRepository employeeRepository;

    @Transactional
    public HrTimesheet generateWeeklyTimesheet(Long orgId, Long employeeId, LocalDate weekDate) {
        LocalDate weekStart = weekDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        HrEmployee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Employee not found"));

        HrTimesheet timesheet = timesheetRepository
                .findByEmployeeIdAndWeekStartDate(employeeId, weekStart)
                .orElseGet(() -> HrTimesheet.builder()
                        .organizationId(orgId)
                        .employee(employee)
                        .weekStartDate(weekStart)
                        .weekEndDate(weekEnd)
                        .build());

        List<HrAttendance> weekAttendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDateBetween(employeeId, weekStart, weekEnd);

        int totalMinutes = weekAttendance.stream().mapToInt(HrAttendance::getTotalWorkMinutes).sum();
        int totalOT = weekAttendance.stream()
                .mapToInt(a -> a.getOvertimeMinutes() != null ? a.getOvertimeMinutes() : 0)
                .sum();

        timesheet.setTotalMinutes(totalMinutes);
        timesheet.setTotalOvertimeMinutes(totalOT);
        return timesheetRepository.save(timesheet);
    }

    @Transactional
    public HrTimesheet submitTimesheet(Long timesheetId) {
        HrTimesheet timesheet = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Timesheet not found"));
        if (!"DRAFT".equals(timesheet.getStatus())) {
            throw new IllegalStateException("Only DRAFT timesheets can be submitted");
        }
        timesheet.setStatus("SUBMITTED");
        timesheet.setSubmittedAt(LocalDateTime.now());
        return timesheetRepository.save(timesheet);
    }

    @Transactional
    public HrTimesheet approveTimesheet(Long timesheetId, Long approvedBy, String remarks) {
        HrTimesheet timesheet = timesheetRepository.findById(timesheetId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Timesheet not found"));
        if (!"SUBMITTED".equals(timesheet.getStatus())) {
            throw new IllegalStateException("Only SUBMITTED timesheets can be approved");
        }
        timesheet.setStatus("APPROVED");
        timesheet.setApprovedBy(approvedBy);
        timesheet.setApprovedAt(LocalDateTime.now());
        timesheet.setRemarks(remarks);
        return timesheetRepository.save(timesheet);
    }

    @Transactional(readOnly = true)
    public List<HrTimesheet> getMyTimesheets(Long employeeId) {
        return timesheetRepository.findByEmployeeIdOrderByWeekStartDateDesc(employeeId);
    }

    @Transactional(readOnly = true)
    public Page<HrTimesheet> getPendingTimesheets(Long orgId, Pageable pageable) {
        return timesheetRepository.findByOrganizationIdAndStatus(orgId, "SUBMITTED", pageable);
    }
}
