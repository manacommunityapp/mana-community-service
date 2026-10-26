package com.manacommunity.api.homeservices.controller;

import com.manacommunity.api.homeservices.model.StaffAttendance;
import com.manacommunity.api.homeservices.repository.StaffAttendanceRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/attendance")
@RequiredArgsConstructor
public class StaffMonthlySummaryController {

    private final StaffAttendanceRepository attendanceRepository;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/monthly-summary")
    public ResponseEntity<List<Map<String, Object>>> getMonthlySummary(
            @RequestParam int month,
            @RequestParam int year,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.getCommunity() == null) return ResponseEntity.ok(List.of());
        Long communityId = user.getCommunity().getId();
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.withDayOfMonth(from.lengthOfMonth());
        List<StaffAttendance> records = attendanceRepository
                .findByStaffCommunityIdAndDateBetweenOrderByStaffNameAsc(communityId, from, to);
        Map<Long, List<StaffAttendance>> byStaff = records.stream()
                .collect(Collectors.groupingBy(a -> a.getStaff().getId()));
        List<Map<String, Object>> summary = byStaff.entrySet().stream().map(e -> {
            List<StaffAttendance> staffRecords = e.getValue();
            StaffAttendance first = staffRecords.get(0);
            long present = staffRecords.stream().filter(a ->
                    a.getStatus() == StaffAttendance.AttendanceStatus.CHECKED_IN ||
                    a.getStatus() == StaffAttendance.AttendanceStatus.CHECKED_OUT).count();
            long absent = staffRecords.stream().filter(a ->
                    a.getStatus() == StaffAttendance.AttendanceStatus.ABSENT).count();
            long onLeave = staffRecords.stream().filter(a ->
                    a.getStatus() == StaffAttendance.AttendanceStatus.ON_LEAVE).count();
            Map<String, Object> row = new HashMap<>();
            row.put("staffId", first.getStaff().getId());
            row.put("staffName", first.getStaff().getName());
            row.put("month", month);
            row.put("year", year);
            row.put("totalDays", to.getDayOfMonth());
            row.put("presentDays", present);
            row.put("absentDays", absent);
            row.put("onLeaveDays", onLeave);
            return row;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(summary);
    }
}
