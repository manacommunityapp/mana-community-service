package com.manacommunity.api.communityops.move;

import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/move-requests")
@RequiredArgsConstructor
public class MoveRequestController {

    private final MoveRequestService service;

    @PostMapping
    public ResponseEntity<MoveRequest> create(@RequestBody MoveRequestDto req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(201).body(service.createRequest(principal.getCommunityId(), principal.getUserId(), req));
    }

    @GetMapping
    public ResponseEntity<List<MoveRequest>> list(
            @RequestParam(required = false) MoveStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getForCommunity(principal.getCommunityId(), status));
    }

    @GetMapping("/my")
    public ResponseEntity<List<MoveRequest>> my(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.getForResident(principal.getUserId()));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<MoveRequest> approve(@PathVariable Long id,
            @RequestBody ReviewRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.approve(id, principal.getUserId(), req.notes()));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<MoveRequest> reject(@PathVariable Long id,
            @RequestBody ReviewRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.reject(id, principal.getUserId(), req.notes()));
    }

    @PutMapping("/{id}/checklist/{itemId}")
    public ResponseEntity<MoveChecklistCompletion> updateChecklist(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @RequestBody ChecklistUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.updateChecklist(id, itemId, req.completed(), principal.getUserId(), req.notes()));
    }

    @GetMapping("/{id}/checklist")
    public ResponseEntity<List<MoveChecklistCompletion>> getChecklist(@PathVariable Long id) {
        return ResponseEntity.ok(service.getChecklist(id));
    }

    @PostMapping("/{id}/noc")
    public ResponseEntity<MoveRequest> issueNoc(@PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(service.issueNoc(id, principal.getUserId()));
    }

    record ReviewRequest(String notes) {}
    record ChecklistUpdateRequest(boolean completed, String notes) {}
}
