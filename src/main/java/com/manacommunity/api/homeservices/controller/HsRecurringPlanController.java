package com.manacommunity.api.homeservices.controller;

import com.manacommunity.api.homeservices.entity.HsRecurringPlan;
import com.manacommunity.api.homeservices.service.HsRecurringPlanService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/home-services/plans")
@RequiredArgsConstructor
public class HsRecurringPlanController {

    private final HsRecurringPlanService planService;

    @PostMapping
    public ResponseEntity<HsRecurringPlan> create(@RequestBody HsRecurringPlan plan,
            @AuthenticationPrincipal UserPrincipal p) {
        plan.setCommunityId(p.getCommunityId());
        plan.setUserId(p.getUserId());
        return ResponseEntity.ok(planService.create(plan));
    }

    @GetMapping("/my")
    public ResponseEntity<List<HsRecurringPlan>> myPlans(@AuthenticationPrincipal UserPrincipal p) {
        return ResponseEntity.ok(planService.myPlans(p.getUserId()));
    }

    @PutMapping("/{id}/pause")
    public ResponseEntity<HsRecurringPlan> pause(@PathVariable Long id) {
        return ResponseEntity.ok(planService.pause(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HsRecurringPlan> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(planService.cancel(id));
    }
}
