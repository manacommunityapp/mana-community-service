package com.manacommunity.api.hrm.service;

import com.manacommunity.api.hrm.dto.*;
import com.manacommunity.api.hrm.entity.*;
import com.manacommunity.api.hrm.enums.AttendanceEventType;
import com.manacommunity.api.hrm.enums.AttendanceStatus;
import com.manacommunity.api.hrm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
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
    private final HrEmployeeShiftRepository employeeShiftRepository;
    private final HrAttendanceCorrectionRepository correctionRepository;

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

        // Determine late status based on shift
        AttendanceStatus status = AttendanceStatus.PRESENT;
        HrEmployeeShift employeeShift = employeeShiftRepository
                .findByEmployeeIdAndShiftDate(employee.getId(), today)
                .orElse(null);
        if (employeeShift != null) {
            HrShift shift = employeeShift.getShift();
            LocalTime shiftStart = shift.getStartTime();
            int grace = shift.getGracePeriodMinutes() != null ? shift.getGracePeriodMinutes() : 0;
            int lateThresh = shift.getLateThresholdMinutes() != null ? shift.getLateThresholdMinutes() : 0;
            LocalTime lateCutoff = shiftStart.plusMinutes(grace).plusMinutes(lateThresh);
            if (now.toLocalTime().isAfter(lateCutoff) && lateThresh > 0) {
                status = AttendanceStatus.LATE;
            }
        }

        if (attendance.getFirstCheckIn() == null) {
            attendance.setFirstCheckIn(now.toLocalTime());
            attendance.setAttendanceStatus(status);
        }
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

        // Sum all check-in/check-out pairs from logs (multi-punch fix)
        int totalWorkMinutes = calculateWorkMinutes(employee.getId(), today);
        attendance.setTotalWorkMinutes(totalWorkMinutes);

        // Calculate overtime
        int otThreshold = employee.getOrganization().getOvertimeThresholdMinutes() != null
                ? employee.getOrganization().getOvertimeThresholdMinutes()
                : 480;
        int overtimeMinutes = Math.max(0, totalWorkMinutes - otThreshold);
        attendance.setOvertimeMinutes(overtimeMinutes);

        attendance = attendanceRepository.save(attendance);
        return toResponse(attendance);
    }

    private int calculateWorkMinutes(Long employeeId, LocalDate date) {
        List<HrAttendanceLog> logs = attendanceLogRepository
                .findByEmployeeIdAndEventTimeBetweenOrderByEventTimeAsc(
                        employeeId,
                        date.atStartOfDay(),
                        date.plusDays(1).atStartOfDay());

        int totalMinutes = 0;
        LocalDateTime lastCheckIn = null;

        for (HrAttendanceLog logEntry : logs) {
            if (logEntry.getEventType() == AttendanceEventType.CHECK_IN) {
                lastCheckIn = logEntry.getEventTime();
            } else if (logEntry.getEventType() == AttendanceEventType.CHECK_OUT && lastCheckIn != null) {
                totalMinutes += (int) Duration.between(lastCheckIn, logEntry.getEventTime()).toMinutes();
                lastCheckIn = null;
            }
        }
        return totalMinutes;
    }

    @Transactional
    public AttendanceResponse correctAttendance(Long orgId, Long attendanceId,
                                                 AttendanceCorrectionRequest request, Long correctedBy) {
        HrAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Attendance record not found"));

        if (!attendance.getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("Attendance record not in your organization");
        }

        correctionRepository.save(HrAttendanceCorrection.builder()
                .organizationId(orgId)
                .attendance(attendance)
                .originalCheckIn(attendance.getFirstCheckIn())
                .originalCheckOut(attendance.getLastCheckOut())
                .originalStatus(attendance.getAttendanceStatus().name())
                .correctedCheckIn(request.getCorrectedCheckIn())
                .correctedCheckOut(request.getCorrectedCheckOut())
                .correctedStatus(request.getCorrectedStatus())
                .reason(request.getReason())
                .correctedBy(correctedBy)
                .build());

        if (request.getCorrectedCheckIn() != null) attendance.setFirstCheckIn(request.getCorrectedCheckIn());
        if (request.getCorrectedCheckOut() != null) attendance.setLastCheckOut(request.getCorrectedCheckOut());
        if (request.getCorrectedStatus() != null) {
            attendance.setAttendanceStatus(AttendanceStatus.valueOf(request.getCorrectedStatus()));
        }
        if (attendance.getFirstCheckIn() != null && attendance.getLastCheckOut() != null) {
            int minutes = (int) Duration.between(attendance.getFirstCheckIn(), attendance.getLastCheckOut()).toMinutes();
            attendance.setTotalWorkMinutes(Math.max(0, minutes));
        }

        return toResponse(attendanceRepository.save(attendance));
    }

    @Transactional
    public AttendanceResponse approveOvertime(Long orgId, Long attendanceId, Long approvedBy) {
        HrAttendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Attendance not found"));
        if (!attendance.getOrganizationId().equals(orgId)) {
            throw new AccessDeniedException("Attendance record not in your organization");
        }
        attendance.setOvertimeApproved(true);
        attendance.setOvertimeApprovedBy(approvedBy);
        return toResponse(attendanceRepository.save(attendance));
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
        long late = attendanceRepository.countByOrganizationIdAndAttendanceDateAndAttendanceStatus(orgId, date, AttendanceStatus.LATE);
        long absent = attendanceRepository.countByOrganizationIdAndAttendanceDateAndAttendanceStatus(orgId, date, AttendanceStatus.ABSENT);
        long onLeave = attendanceRepository.countByOrganizationIdAndAttendanceDateAndAttendanceStatus(orgId, date, AttendanceStatus.ON_LEAVE);
        long halfDay = attendanceRepository.countByOrganizationIdAndAttendanceDateAndAttendanceStatus(orgId, date, AttendanceStatus.HALF_DAY);

        return AttendanceSummaryResponse.builder()
                .totalPresent(present + late)
                .totalAbsent(absent)
                .totalOnLeave(onLeave)
                .totalHalfDay(halfDay)
                .totalWorkingDays(present + late + absent + onLeave + halfDay)
                .build();
    }

    private AttendanceResponse toResponse(HrAttendance att) {
        return AttendanceResponse.builder()
                .date(att.getAttendanceDate())
                .firstCheckIn(att.getFirstCheckIn())
                .lastCheckOut(att.getLastCheckOut())
                .totalWorkMinutes(att.getTotalWorkMinutes())
                .status(att.getAttendanceStatus())
                .overtimeMinutes(att.getOvertimeMinutes())
                .overtimeApproved(att.getOvertimeApproved())
                .remarks(att.getRemarks())
                .build();
    }
}
