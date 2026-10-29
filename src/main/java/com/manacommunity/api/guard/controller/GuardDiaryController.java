package com.manacommunity.api.guard.controller;

import com.manacommunity.api.guard.entity.GuardDailyLog;
import com.manacommunity.api.guard.repository.GuardDailyLogRepository;
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
import java.util.List;

@RestController
@RequestMapping("/api/guard/diary")
@RequiredArgsConstructor
public class GuardDiaryController {

    private final GuardDailyLogRepository repo;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    public ResponseEntity<GuardDailyLog> addLog(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody LogRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        GuardDailyLog log = repo.save(GuardDailyLog.builder()
                .communityId(caller.getCommunity().getId())
                .guardUserId(caller.getId())
                .logDate(request.getLogDate() != null ? request.getLogDate() : LocalDate.now())
                .shiftType(request.getShiftType() != null ? request.getShiftType() : "MORNING")
                .entry(request.getEntry())
                .category(request.getCategory() != null ? request.getCategory() : "GENERAL")
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(log);
    }

    @GetMapping
    public ResponseEntity<List<GuardDailyLog>> getLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AppUser caller = loggedInUserService.resolve(principal);
        Long communityId = caller.getCommunity().getId();
        if (date != null) {
            return ResponseEntity.ok(repo.findByCommunityIdAndLogDateOrderByCreatedAtDesc(communityId, date));
        }
        return ResponseEntity.ok(repo.findByCommunityIdOrderByLogDateDescCreatedAtDesc(communityId));
    }

    @Data
    public static class LogRequest {
        private LocalDate logDate;
        private String shiftType;
        private String entry;
        private String category;
    }
}
