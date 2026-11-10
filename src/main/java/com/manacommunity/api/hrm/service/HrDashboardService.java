package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.dto.HrDashboardResponse;
import com.manacommunity.api.hrm.enums.AttendanceStatus;
import com.manacommunity.api.hrm.enums.EmploymentStatus;
import com.manacommunity.api.hrm.enums.LeaveApprovalStatus;
import com.manacommunity.api.hrm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HrDashboardService {

    private final HrEmployeeRepository employeeRepository;
    private final HrAttendanceRepository attendanceRepository;
    private final HrLeaveRequestRepository leaveRequestRepository;
    private final HrHolidayRepository holidayRepository;

    @Transactional(readOnly = true)
    public HrDashboardResponse getDashboard(Long orgId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        long totalEmployees = employeeRepository.countByOrganizationId(orgId);
        long activeEmployees = employeeRepository.countByOrganizationIdAndEmploymentStatus(orgId, EmploymentStatus.ACTIVE);
        long presentToday = attendanceRepository.countByOrganizationIdAndAttendanceDateAndAttendanceStatus(orgId, today, AttendanceStatus.PRESENT);
        long absentToday = attendanceRepository.countByOrganizationIdAndAttendanceDateAndAttendanceStatus(orgId, today, AttendanceStatus.ABSENT);
        long onLeaveToday = attendanceRepository.countByOrganizationIdAndAttendanceDateAndAttendanceStatus(orgId, today, AttendanceStatus.ON_LEAVE);
        long pendingLeaveApprovals = leaveRequestRepository.countByOrganizationIdAndApprovalStatus(orgId, LeaveApprovalStatus.PENDING);

        List<HrDashboardResponse.HolidayInfo> upcomingHolidays = holidayRepository
                .findByOrganizationIdAndHolidayDateBetween(orgId, today, today.plusDays(30))
                .stream()
                .map(h -> HrDashboardResponse.HolidayInfo.builder()
                        .holidayName(h.getHolidayName())
                        .holidayDate(h.getHolidayDate().toString())
                        .isOptional(h.getIsOptional())
                        .build())
                .toList();

        return HrDashboardResponse.builder()
                .totalEmployees(totalEmployees)
                .activeEmployees(activeEmployees)
                .presentToday(presentToday)
                .absentToday(absentToday)
                .onLeaveToday(onLeaveToday)
                .pendingLeaveApprovals(pendingLeaveApprovals)
                .newJoinersThisMonth(0)
                .upcomingHolidays(upcomingHolidays)
                .build();
    }
}
