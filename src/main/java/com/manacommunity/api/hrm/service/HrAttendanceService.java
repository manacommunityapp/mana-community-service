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
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HrAttendanceService {

    private final HrAttendanceRepository attendanceRepository;
    private final HrAttendanceLogRepository attendanceLogRepository;
    private final HrAttendanceSessionRepository attendanceSessionRepository;
    private final HrEmployeeRepository employeeRepository;
    private final HrEmployeeShiftRepository employeeShiftRepository;
    private final HrAttendanceCorrectionRepository correctionRepository;

    @Transactional
    public AttendanceResponse checkIn(HrEmployee employee, CheckInRequest request) {
        // P0: pessimistic lock on employee to prevent race conditions
        employeeRepository.findByIdAndOrganizationIdWithLock(employee.getId(), employee.getOrganization().getId())
                .orElseThrow(() -> new IllegalStateException("Employee not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // P0: idempotency — reject if already checked in today
        if (attendanceRepository.existsByEmployeeIdAndAttendanceDate(employee.getId(), today)) {
            throw new IllegalStateException("Already checked in today");
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

        HrAttendance attendance = HrAttendance.builder()
                .organizationId(employee.getOrganization().getId())
                .employee(employee)
                .attendanceDate(today)
                .firstCheckIn(now.toLocalTime())
                .attendanceStatus(status)
                .build();
        attendance = attendanceRepository.save(attendance);

        // P1: create first session
        HrAttendanceSession session = HrAttendanceSession.builder()
                .attendance(attendance)
                .sessionIn(now)
                .build();
        attendanceSessionRepository.save(session);

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

        // P1: close the open session
        Optional<HrAttendanceSession> openSession = attendanceSessionRepository
                .findTopByAttendanceIdAndSessionOutIsNullOrderBySessionInDesc(attendance.getId());
        if (openSession.isPresent()) {
            HrAttendanceSession session = openSession.get();
            session.setSessionOut(now);
            attendanceSessionRepository.save(session);
        }

        // P1: night shift detection
        if (attendance.getFirstCheckIn() != null && now.toLocalDate().isAfter(attendance.getAttendanceDate())) {
            attendance.setIsNightShift(true);
        }

        attendance.setLastCheckOut(now.toLocalTime());

        // P1: recalculate total work minutes from all sessions
        int totalWorkMinutes = calculateWorkMinutesFromSessions(attendance.getId());
        attendance.setTotalWorkMinutes(totalWorkMinutes);

        // P1: overtime calculation vs standard shift
        int standardMinutes = getStandardShiftMinutes(employee.getOrganization().getId(), employee.getId(), today);
        attendance.setOvertimeMinutes(Math.max(0, totalWorkMinutes - standardMinutes));

        attendance = attendanceRepository.save(attendance);
        return toResponse(attendance);
    }

    // P1: start a break (closes current session, opens a new one on return)
    @Transactional
    public AttendanceResponse startBreak(HrEmployee employee) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        HrAttendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new IllegalStateException("No check-in found for today"));

        // Close the open session at break start
        attendanceSessionRepository
                .findTopByAttendanceIdAndSessionOutIsNullOrderBySessionInDesc(attendance.getId())
                .ifPresent(session -> {
                    session.setSessionOut(now);
                    attendanceSessionRepository.save(session);
                });

        return toResponse(attendance);
    }

    // P1: end a break (opens a new session)
    @Transactional
    public AttendanceResponse endBreak(HrEmployee employee) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        HrAttendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new IllegalStateException("No check-in found for today"));

        HrAttendanceSession newSession = HrAttendanceSession.builder()
                .attendance(attendance)
                .sessionIn(now)
                .build();
        attendanceSessionRepository.save(newSession);

        return toResponse(attendance);
    }

    private int calculateWorkMinutesFromSessions(Long attendanceId) {
        List<HrAttendanceSession> sessions = attendanceSessionRepository
                .findByAttendanceIdOrderBySessionInAsc(attendanceId);
        int total = 0;
        for (HrAttendanceSession s : sessions) {
            if (s.getSessionOut() != null) {
                total += (int) Duration.between(s.getSessionIn(), s.getSessionOut()).toMinutes();
            }
        }
        return total;
    }

    private int getStandardShiftMinutes(Long orgId, Long employeeId, LocalDate date) {
        return employeeShiftRepository.findByEmployeeIdAndShiftDate(employeeId, date)
                .map(es -> {
                    HrShift shift = es.getShift();
                    if (shift.getStartTime() != null && shift.getEndTime() != null) {
                        int minutes = (int) Duration.between(shift.getStartTime(), shift.getEndTime()).toMinutes();
                        // Handle overnight shifts
                        if (minutes < 0) minutes += 24 * 60;
                        int breakMins = shift.getBreakMinutes() != null ? shift.getBreakMinutes() : 0;
                        return Math.max(0, minutes - breakMins);
                    }
                    return 480;
                })
                .orElse(480);
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

    // P0: manager-authorised employee-specific attendance
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByEmployee(Long orgId, Long employeeId, String month) {
        LocalDate from;
        LocalDate to;
        if (month != null && !month.isBlank()) {
            YearMonth ym = YearMonth.parse(month);
            from = ym.atDay(1);
            to = ym.atEndOfMonth();
        } else {
            to = LocalDate.now();
            from = to.minusDays(29);
        }
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
