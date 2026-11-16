package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.dto.*;
import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.enums.AttendanceEventType;
import com.manacommunity.api.hrm.enums.AttendanceStatus;
import com.manacommunity.api.hrm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HrAttendanceService {

    private final HrAttendanceRepository attendanceRepository;
    private final HrAttendanceLogRepository attendanceLogRepository;
    private final HrEmployeeRepository employeeRepository;

    @Transactional
    public AttendanceResponse checkIn(HrEmployee employee, CheckInRequest request) {
        LocalDateTime now = LocalDateTime.now();

        Optional<HrAttendanceLog> lastCheckIn = attendanceLogRepository
                .findTopByEmployeeIdAndEventTypeOrderByEventTimeDesc(employee.getId(), AttendanceEventType.CHECK_IN);
        if (lastCheckIn.isPresent()) {
            Duration sinceLastCheckIn = Duration.between(lastCheckIn.get().getEventTime(), now);
            if (sinceLastCheckIn.toMinutes() < 1) {
                throw new IllegalStateException("Duplicate check-in detected. Please wait before trying again.");
            }
        }

        HrAttendanceLog log = HrAttendanceLog.builder()
                .organizationId(employee.getOrganization().getId())
                .employee(employee)
                .eventType(AttendanceEventType.CHECK_IN)
                .eventTime(now)
                .source(request.getSource())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .deviceId(request.getDeviceId())
                .build();
        attendanceLogRepository.save(log);

        LocalDate today = now.toLocalDate();
        HrAttendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseGet(() -> HrAttendance.builder()
                        .organizationId(employee.getOrganization().getId())
                        .employee(employee)
                        .attendanceDate(today)
                        .attendanceStatus(AttendanceStatus.PRESENT)
                        .build());

        if (attendance.getFirstCheckIn() == null) {
            attendance.setFirstCheckIn(now.toLocalTime());
        }
        attendance.setAttendanceStatus(AttendanceStatus.PRESENT);
        attendance = attendanceRepository.save(attendance);

        return toResponse(attendance);
    }

    @Transactional
    public AttendanceResponse checkOut(HrEmployee employee, CheckOutRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        HrAttendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new IllegalStateException("No check-in found for today. Please check in first."));

        HrAttendanceLog log = HrAttendanceLog.builder()
                .organizationId(employee.getOrganization().getId())
                .employee(employee)
                .eventType(AttendanceEventType.CHECK_OUT)
                .eventTime(now)
                .source(request.getSource())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .deviceId(request.getDeviceId())
                .build();
        attendanceLogRepository.save(log);

        attendance.setLastCheckOut(now.toLocalTime());

        if (attendance.getFirstCheckIn() != null) {
            long minutes = Duration.between(attendance.getFirstCheckIn(), now.toLocalTime()).toMinutes();
            attendance.setTotalWorkMinutes((int) Math.max(minutes, 0));
        }

        attendance = attendanceRepository.save(attendance);
        return toResponse(attendance);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getMyAttendance(Long employeeId, LocalDate from, LocalDate to) {
        return attendanceRepository.findByEmployeeIdAndAttendanceDateBetween(employeeId, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getAttendanceReport(Long orgId, LocalDate date, Pageable pageable) {
        return attendanceRepository.findByOrganizationIdAndAttendanceDate(orgId, date, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getDailySummary(Long orgId, LocalDate date) {
        long present = attendanceRepository.countByOrganizationIdAndAttendanceDateAndAttendanceStatus(orgId, date, AttendanceStatus.PRESENT);
        long absent = attendanceRepository.countByOrganizationIdAndAttendanceDateAndAttendanceStatus(orgId, date, AttendanceStatus.ABSENT);
        long onLeave = attendanceRepository.countByOrganizationIdAndAttendanceDateAndAttendanceStatus(orgId, date, AttendanceStatus.ON_LEAVE);
        long halfDay = attendanceRepository.countByOrganizationIdAndAttendanceDateAndAttendanceStatus(orgId, date, AttendanceStatus.HALF_DAY);

        return AttendanceSummaryResponse.builder()
                .totalPresent(present)
                .totalAbsent(absent)
                .totalOnLeave(onLeave)
                .totalHalfDay(halfDay)
                .totalWorkingDays(present + absent + onLeave + halfDay)
                .build();
    }

    private AttendanceResponse toResponse(HrAttendance att) {
        return AttendanceResponse.builder()
                .date(att.getAttendanceDate())
                .firstCheckIn(att.getFirstCheckIn())
                .lastCheckOut(att.getLastCheckOut())
                .totalWorkMinutes(att.getTotalWorkMinutes())
                .status(att.getAttendanceStatus())
                .remarks(att.getRemarks())
                .build();
    }
}
