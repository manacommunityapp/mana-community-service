package com.manacommunity.api.staff.controller;

import com.manacommunity.api.staff.entity.StaffAccessRule;
import com.manacommunity.api.staff.service.StaffAccessRuleService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/staff/access-rules")
@RequiredArgsConstructor
public class StaffAccessRuleController {

    private final StaffAccessRuleService staffAccessRuleService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/{staffId}")
    public ResponseEntity<List<StaffAccessRule>> getRules(@PathVariable Long staffId) {
        return ResponseEntity.ok(staffAccessRuleService.getRules(staffId));
    }

    @PostMapping
    public ResponseEntity<StaffAccessRule> setRule(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AccessRuleRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        StaffAccessRule rule = staffAccessRuleService.setRule(
                caller.getCommunity().getId(),
                request.getStaffId(),
                request.getDayOfWeek(),
                request.getAllowedFrom(),
                request.getAllowedUntil());
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long id) {
        staffAccessRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class AccessRuleRequest {
        private Long staffId;
        private String dayOfWeek;
        private LocalTime allowedFrom;
        private LocalTime allowedUntil;
    }
}
