package com.manacommunity.api.homeservices.service.impl;

import com.manacommunity.api.homeservices.dto.AttendanceSummaryResponse;
import com.manacommunity.api.homeservices.dto.StaffAttendanceRequest;
import com.manacommunity.api.homeservices.dto.StaffAttendanceResponse;
import com.manacommunity.api.homeservices.model.DomesticStaff;
import com.manacommunity.api.homeservices.model.StaffAttendance;
import com.manacommunity.api.homeservices.repository.DomesticStaffRepository;
import com.manacommunity.api.homeservices.repository.StaffAttendanceRepository;
import com.manacommunity.api.homeservices.service.StaffAttendanceService;
import com.manacommunity.api.user.model.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffAttendanceServiceImpl implements StaffAttendanceService {

    private final StaffAttendanceRepository attendanceRepository;
    private final DomesticStaffRepository staffRepository;

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

        return toResponse(attendanceRepository.save(attendance));
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
