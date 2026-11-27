package com.manacommunity.api.healthcare.controller;

import com.manacommunity.api.healthcare.entity.PrescriptionFollowup;
import com.manacommunity.api.healthcare.service.PrescriptionFollowupService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/healthcare/followups")
@RequiredArgsConstructor
public class PrescriptionFollowupController {

    private final PrescriptionFollowupService followupService;

    @PostMapping
    public ResponseEntity<PrescriptionFollowup> create(
            @RequestBody PrescriptionFollowup followup,
            @AuthenticationPrincipal UserPrincipal principal) {
        followup.setCommunityId(principal.getCommunityId());
        return ResponseEntity.status(201).body(followupService.create(followup));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<PrescriptionFollowup> complete(@PathVariable Long id) {
        return ResponseEntity.ok(followupService.markComplete(id));
    }

    @GetMapping({"", "/pending"})
    public ResponseEntity<List<PrescriptionFollowup>> pending(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String date,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long cid = communityId != null ? communityId : (principal != null ? principal.getCommunityId() : null);
        return ResponseEntity.ok(followupService.getPendingForCommunity(cid));
    }

    @GetMapping("/patient/{patientUserId}")
    public ResponseEntity<List<PrescriptionFollowup>> forPatient(
            @PathVariable Long patientUserId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(followupService.getForPatient(principal.getCommunityId(), patientUserId));
    }
}
