package com.manacommunity.api.staff.controller;

import com.manacommunity.api.staff.entity.StaffLeave;
import com.manacommunity.api.staff.repository.StaffLeaveRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/staff/leave")
@RequiredArgsConstructor
public class StaffLeaveController {

    private final StaffLeaveRepository repo;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    public ResponseEntity<StaffLeave> markLeave(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody LeaveRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        StaffLeave leave = repo.save(StaffLeave.builder()
                .communityId(caller.getCommunity().getId())
                .staffId(request.getStaffId())
                .residentUserId(caller.getId())
                .leaveDate(request.getLeaveDate())
                .reason(request.getReason())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(leave);
    }

    @GetMapping("/{staffId}")
    public ResponseEntity<List<StaffLeave>> getLeaves(
            @PathVariable Long staffId,
            @RequestParam(required = false) String month) {
        if (month != null) {
            YearMonth ym = YearMonth.parse(month);
            return ResponseEntity.ok(repo.findByStaffIdAndLeaveDateBetweenOrderByLeaveDate(
                    staffId, ym.atDay(1), ym.atEndOfMonth()));
        }
        return ResponseEntity.ok(repo.findByStaffIdAndLeaveDateBetweenOrderByLeaveDate(
                staffId, LocalDate.now().withDayOfMonth(1), LocalDate.now()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelLeave(@PathVariable Long id) {
        repo.findById(id).ifPresent(l -> {
            l.setStatus("CANCELLED");
            repo.save(l);
        });
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class LeaveRequest {
        private Long staffId;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate leaveDate;
        private String reason;
    }
}
