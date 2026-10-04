package com.manacommunity.api.safety.controller;

import com.manacommunity.api.safety.dto.PatrolCheckpointResponse;
import com.manacommunity.api.safety.dto.PatrolLogResponse;
import com.manacommunity.api.safety.dto.PatrolScanRequest;
import com.manacommunity.api.safety.service.PatrolService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety/patrol")
@RequiredArgsConstructor
public class PatrolController {

    private final PatrolService patrolService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/checkpoints")
    public ResponseEntity<List<PatrolCheckpointResponse>> getCheckpoints(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(patrolService.getCheckpoints(communityId));
    }

    @PostMapping("/scan")
    public ResponseEntity<PatrolLogResponse> scanCheckpoint(
            @Valid @RequestBody PatrolScanRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(patrolService.scanCheckpoint(request, user));
    }

    @GetMapping("/logs")
    public ResponseEntity<List<PatrolLogResponse>> getPatrolLogs(
            @RequestParam Long shiftId,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(patrolService.getPatrolLogs(shiftId));
    }
}
