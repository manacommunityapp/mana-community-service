package com.manacommunity.api.fm.ops;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fm/dashboard")
@PreAuthorize("hasAnyRole('FM_ADMIN','FACILITY_MANAGER','SUPER_ADMIN','PLATFORM_ADMIN')")
@RequiredArgsConstructor
public class FmOpsDashboardController {

    private final FmOpsDashboardService service;

    @GetMapping("/snapshot")
    public ResponseEntity<FmOpsDashboardSnapshot> snapshot(
        @RequestParam long communityId
    ) {
        return ResponseEntity.ok(service.getSnapshot(communityId));
    }

    @GetMapping("/complaints")
    public ResponseEntity<FmOpsDashboardSnapshot.ComplaintQueueMetrics> complaints(
        @RequestParam long communityId
    ) {
        return ResponseEntity.ok(service.getSnapshot(communityId).complaintQueue());
    }

    @GetMapping("/amenities")
    public ResponseEntity<FmOpsDashboardSnapshot.AmenityUtilizationMetrics> amenities(
        @RequestParam long communityId
    ) {
        return ResponseEntity.ok(service.getSnapshot(communityId).amenityUtilization());
    }

    @GetMapping("/dues")
    public ResponseEntity<FmOpsDashboardSnapshot.DuesCollectionMetrics> dues(
        @RequestParam long communityId
    ) {
        return ResponseEntity.ok(service.getSnapshot(communityId).duesCollection());
    }

    @GetMapping("/events")
    public ResponseEntity<FmOpsDashboardSnapshot.EventPipelineMetrics> events(
        @RequestParam long communityId
    ) {
        return ResponseEntity.ok(service.getSnapshot(communityId).eventPipeline());
    }

    @GetMapping("/security")
    public ResponseEntity<FmOpsDashboardSnapshot.SecurityLogMetrics> security(
        @RequestParam long communityId
    ) {
        return ResponseEntity.ok(service.getSnapshot(communityId).securityLog());
    }

    @GetMapping("/staff")
    public ResponseEntity<FmOpsDashboardSnapshot.StaffRosterMetrics> staff(
        @RequestParam long communityId
    ) {
        return ResponseEntity.ok(service.getSnapshot(communityId).staffRoster());
    }

    @GetMapping("/budget")
    public ResponseEntity<FmOpsDashboardSnapshot.BudgetHealthMetrics> budget(
        @RequestParam long communityId
    ) {
        return ResponseEntity.ok(service.getSnapshot(communityId).budgetHealth());
    }
}
