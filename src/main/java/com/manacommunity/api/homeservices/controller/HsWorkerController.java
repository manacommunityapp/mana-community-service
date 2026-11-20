package com.manacommunity.api.homeservices.controller;

import com.manacommunity.api.homeservices.entity.*;
import com.manacommunity.api.homeservices.service.HsWorkerService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/home-services/workers")
@RequiredArgsConstructor
public class HsWorkerController {

    private final HsWorkerService workerService;

    @GetMapping
    public ResponseEntity<List<HsWorker>> list(@AuthenticationPrincipal UserPrincipal p,
            @RequestParam(required = false) String category) {
        if (category != null)
            return ResponseEntity.ok(workerService.findByCategory(p.getCommunityId(), category));
        return ResponseEntity.ok(workerService.listActive(p.getCommunityId()));
    }

    @GetMapping("/available")
    public ResponseEntity<List<HsWorkerAvailability>> availableSlots(
            @AuthenticationPrincipal UserPrincipal p,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(workerService.availableSlots(p.getCommunityId(), date));
    }

    @PostMapping("/{workerId}/availability")
    public ResponseEntity<HsWorkerAvailability> addAvailability(@PathVariable Long workerId,
            @RequestBody HsWorkerAvailability slot,
            @AuthenticationPrincipal UserPrincipal p) {
        slot.setWorkerId(workerId);
        slot.setCommunityId(p.getCommunityId());
        return ResponseEntity.ok(workerService.addAvailability(slot));
    }
}
