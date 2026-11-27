package com.manacommunity.api.communityops.attendance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffAttendanceService {

    private final StaffShiftRepository shiftRepo;
    private final StaffLeaveRequestRepository leaveRepo;

    @Transactional
    public StaffShift clockIn(Long communityId, Long staffUserId, Long markedBy) {
        LocalDate today = LocalDate.now();
        StaffShift shift = shiftRepo
            .findByCommunityIdAndStaffUserIdAndShiftDate(communityId, staffUserId, today)
            .orElseGet(() -> StaffShift.builder()
                .communityId(communityId)
                .staffUserId(staffUserId)
                .shiftDate(today)
                .expectedStart(LocalTime.of(9, 0))
                .expectedEnd(LocalTime.of(18, 0))
                .build());
        shift.setActualStart(Instant.now());
        shift.setStatus(AttendanceStatus.PRESENT);
        shift.setMarkedBy(markedBy);
        return shiftRepo.save(shift);
    }

    @Transactional
    public StaffShift clockOut(Long communityId, Long staffUserId) {
        LocalDate today = LocalDate.now();
        StaffShift shift = shiftRepo
            .findByCommunityIdAndStaffUserIdAndShiftDate(communityId, staffUserId, today)
            .orElseThrow(() -> new IllegalStateException("No shift found for today"));
        shift.setActualEnd(Instant.now());
        return shiftRepo.save(shift);
    }

    @Transactional
    public StaffShift markAttendance(Long communityId, Long staffUserId, AttendanceStatus status, Long markedBy, String notes) {
        LocalDate today = LocalDate.now();
        StaffShift shift = shiftRepo
            .findByCommunityIdAndStaffUserIdAndShiftDate(communityId, staffUserId, today)
            .orElseGet(() -> StaffShift.builder()
                .communityId(communityId)
                .staffUserId(staffUserId)
                .shiftDate(today)
                .expectedStart(LocalTime.of(9, 0))
                .expectedEnd(LocalTime.of(18, 0))
                .build());
        shift.setStatus(status);
        shift.setMarkedBy(markedBy);
        shift.setNotes(notes);
        return shiftRepo.save(shift);
    }

    public List<StaffShift> getReport(Long communityId, LocalDate from, LocalDate to) {
        return shiftRepo.findByCommunityIdAndShiftDateBetween(communityId, from, to);
    }

    public Map<AttendanceStatus, Long> getSummary(Long communityId, LocalDate from, LocalDate to) {
        return shiftRepo.findByCommunityIdAndShiftDateBetween(communityId, from, to).stream()
            .collect(Collectors.groupingBy(StaffShift::getStatus, Collectors.counting()));
    }

    @Transactional
    public StaffLeaveRequest createLeaveRequest(Long communityId, Long staffUserId, LeaveType leaveType,
            LocalDate fromDate, LocalDate toDate, String reason) {
        return leaveRepo.save(StaffLeaveRequest.builder()
            .communityId(communityId)
            .staffUserId(staffUserId)
            .leaveType(leaveType)
            .fromDate(fromDate)
            .toDate(toDate)
            .reason(reason)
            .status(LeaveStatus.PENDING)
            .build());
    }

    @Transactional
    public StaffLeaveRequest reviewLeave(Long leaveId, Long reviewerId, boolean approved, String notes) {
        StaffLeaveRequest leave = leaveRepo.findById(leaveId)
            .orElseThrow(() -> new IllegalArgumentException("Leave request not found"));
        leave.setStatus(approved ? LeaveStatus.APPROVED : LeaveStatus.REJECTED);
        leave.setReviewedBy(reviewerId);
        leave.setReviewedAt(Instant.now());
        leave.setReviewNotes(notes);
        return leaveRepo.save(leave);
    }

    public List<StaffLeaveRequest> getPendingLeaves(Long communityId) {
        return leaveRepo.findByCommunityIdAndStatus(communityId, LeaveStatus.PENDING);
    }
}
