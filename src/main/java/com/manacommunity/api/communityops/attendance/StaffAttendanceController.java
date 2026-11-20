package com.manacommunity.api.communityops.attendance;

import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/staff/attendance")
@RequiredArgsConstructor
public class StaffAttendanceController {

    private final StaffAttendanceService service;

    @PostMapping("/clock-in")
    public ResponseEntity<StaffShift> clockIn(@RequestBody ClockRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.clockIn(principal.getCommunityId(), req.staffUserId(), principal.getUserId()));
    }

    @PostMapping("/clock-out")
    public ResponseEntity<StaffShift> clockOut(@RequestBody ClockRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.clockOut(principal.getCommunityId(), req.staffUserId()));
    }

    @PostMapping("/mark")
    public ResponseEntity<StaffShift> mark(@RequestBody MarkAttendanceRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.markAttendance(principal.getCommunityId(),
            req.staffUserId(), req.status(), principal.getUserId(), req.notes()));
    }

    @GetMapping("/report")
    public ResponseEntity<List<StaffShift>> report(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getReport(principal.getCommunityId(), from, to));
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<AttendanceStatus, Long>> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getSummary(principal.getCommunityId(), from, to));
    }

    @PostMapping("/leave")
    public ResponseEntity<StaffLeaveRequest> applyLeave(@RequestBody LeaveRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(201).body(service.createLeaveRequest(principal.getCommunityId(),
            req.staffUserId(), req.leaveType(), req.fromDate(), req.toDate(), req.reason()));
    }

    @PutMapping("/leave/{id}/review")
    public ResponseEntity<StaffLeaveRequest> reviewLeave(@PathVariable Long id,
            @RequestBody LeaveReviewRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.reviewLeave(id, principal.getUserId(), req.approved(), req.notes()));
    }

    @GetMapping("/leave/pending")
    public ResponseEntity<List<StaffLeaveRequest>> pendingLeaves(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getPendingLeaves(principal.getCommunityId()));
    }

    record ClockRequest(Long staffUserId) {}
    record MarkAttendanceRequest(Long staffUserId, AttendanceStatus status, String notes) {}
    record LeaveRequest(Long staffUserId, LeaveType leaveType, LocalDate fromDate, LocalDate toDate, String reason) {}
    record LeaveReviewRequest(boolean approved, String notes) {}
}
