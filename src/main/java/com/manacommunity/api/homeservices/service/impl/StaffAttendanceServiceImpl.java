package com.manacommunity.api.homeservices.service.impl;

import com.manacommunity.api.homeservices.dto.AttendanceSummaryResponse;
import com.manacommunity.api.homeservices.dto.StaffAttendanceRequest;
import com.manacommunity.api.homeservices.dto.StaffAttendanceResponse;
import com.manacommunity.api.homeservices.model.DomesticStaff;
import com.manacommunity.api.homeservices.model.StaffAttendance;
import com.manacommunity.api.homeservices.model.StaffHouseholdLink;
import com.manacommunity.api.homeservices.repository.DomesticStaffRepository;
import com.manacommunity.api.homeservices.repository.StaffAttendanceRepository;
import com.manacommunity.api.homeservices.repository.StaffHouseholdLinkRepository;
import com.manacommunity.api.homeservices.service.StaffAttendanceService;
import com.manacommunity.api.model.NotificationCategory;
import com.manacommunity.api.model.NotificationPriority;
import com.manacommunity.api.model.NotificationType;
import com.manacommunity.api.model.ReferenceType;
import com.manacommunity.api.service.NotificationManagementService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffAttendanceServiceImpl implements StaffAttendanceService {

    private final StaffAttendanceRepository attendanceRepository;
    private final DomesticStaffRepository staffRepository;

    @Autowired(required = false)
    private StaffHouseholdLinkRepository householdLinkRepository;

    @Autowired(required = false)
    private NotificationManagementService notificationService;

    @Override
    @Transactional(readOnly = true)
    public List<StaffAttendanceResponse> getAttendance(Long communityId, LocalDate date) {
        return attendanceRepository.findByStaffCommunityIdAndDateOrderByStaffNameAsc(communityId, date)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceSummaryResponse> getAttendanceSummary(Long communityId, LocalDate from, LocalDate to) {
        List<StaffAttendance> records = attendanceRepository
                .findByStaffCommunityIdAndDateBetweenOrderByStaffNameAsc(communityId, from, to);

        Map<Long, List<StaffAttendance>> byStaff = records.stream()
                .collect(Collectors.groupingBy(a -> a.getStaff().getId()));

        return byStaff.entrySet().stream().map(entry -> {
            List<StaffAttendance> staffRecords = entry.getValue();
            DomesticStaff staff = staffRecords.get(0).getStaff();

            AttendanceSummaryResponse summary = new AttendanceSummaryResponse();
            summary.setStaffId(staff.getId());
            summary.setStaffName(staff.getName());
            summary.setRole(staff.getRole().name());
            summary.setTotalPresent(staffRecords.stream()
                    .filter(a -> a.getStatus() == StaffAttendance.AttendanceStatus.CHECKED_IN
                            || a.getStatus() == StaffAttendance.AttendanceStatus.CHECKED_OUT)
                    .count());
            summary.setTotalAbsent(staffRecords.stream()
                    .filter(a -> a.getStatus() == StaffAttendance.AttendanceStatus.ABSENT).count());
            summary.setTotalOnLeave(staffRecords.stream()
                    .filter(a -> a.getStatus() == StaffAttendance.AttendanceStatus.ON_LEAVE).count());
            summary.setTotalNotMarked(staffRecords.stream()
                    .filter(a -> a.getStatus() == StaffAttendance.AttendanceStatus.NOT_MARKED).count());
            return summary;
        }).toList();
    }

    @Override
    @Transactional
    public StaffAttendanceResponse markAttendance(StaffAttendanceRequest request, AppUser markedBy) {
        DomesticStaff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new RuntimeException("Staff not found with id: " + request.getStaffId()));

        StaffAttendance attendance = StaffAttendance.builder()
                .staff(staff)
                .date(request.getDate())
                .status(request.getStatus())
                .checkInTime(request.getCheckInTime())
                .checkOutTime(request.getCheckOutTime())
                .checkInGate(request.getCheckInGate())
                .checkOutGate(request.getCheckOutGate())
                .markedBy(markedBy)
                .build();

        StaffAttendance saved = attendanceRepository.save(attendance);
        notifyLinkedHouseholds(staff, saved);
        return toResponse(saved);
    }

    private void notifyLinkedHouseholds(DomesticStaff staff, StaffAttendance attendance) {
        if (householdLinkRepository == null || notificationService == null || staff == null) {
            return;
        }
        if (attendance.getStatus() != StaffAttendance.AttendanceStatus.CHECKED_IN
                && attendance.getStatus() != StaffAttendance.AttendanceStatus.CHECKED_OUT) {
            return;
        }
        boolean entered = attendance.getStatus() == StaffAttendance.AttendanceStatus.CHECKED_IN;
        String gate = entered ? attendance.getCheckInGate() : attendance.getCheckOutGate();
        String title = entered ? "Daily Staff Entered" : "Daily Staff Exited";
        String msg = String.format("%s (%s) %s %s.",
                staff.getName(),
                staff.getRole() != null ? staff.getRole().name() : "STAFF",
                entered ? "checked in at" : "checked out from",
                gate != null && !gate.isBlank() ? gate : "Main Gate");
        Long commId = staff.getCommunity() != null ? staff.getCommunity().getId() : null;

        Set<Long> notified = new LinkedHashSet<>();
        for (StaffHouseholdLink link : householdLinkRepository.findByStaffIdAndActiveTrue(staff.getId())) {
            if (link.getResidentUserId() != null && notified.add(link.getResidentUserId())) {
                try {
                    notificationService.createNotification(
                            link.getResidentUserId(),
                            entered ? NotificationType.VISITOR_CHECK_IN : NotificationType.VISITOR_CHECK_OUT,
                            NotificationCategory.COMMUNITY,
                            title,
                            msg,
                            "/daily-help",
                            ReferenceType.COMMUNITY,
                            staff.getId(),
                            NotificationPriority.NORMAL,
                            null,
                            commId
                    );
                } catch (Exception ex) {
                    log.warn("Failed household staff alert for user {}: {}", link.getResidentUserId(), ex.getMessage());
                }
            }
        }
    }

    private StaffAttendanceResponse toResponse(StaffAttendance attendance) {
        StaffAttendanceResponse response = new StaffAttendanceResponse();
        response.setId(attendance.getId());
        response.setStaffId(attendance.getStaff().getId());
        response.setStaffName(attendance.getStaff().getName());
        response.setDate(attendance.getDate());
        response.setStatus(attendance.getStatus().name());
        response.setCheckInTime(attendance.getCheckInTime());
        response.setCheckOutTime(attendance.getCheckOutTime());
        response.setCheckInGate(attendance.getCheckInGate());
        response.setCheckOutGate(attendance.getCheckOutGate());
        response.setMarkedByName(attendance.getMarkedBy() != null ? attendance.getMarkedBy().getFullName() : null);
        response.setCreatedAt(attendance.getCreatedAt());
        return response;
    }
}
