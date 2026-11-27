package com.manacommunity.api.sync;

import com.manacommunity.api.sync.dto.SyncDtos.*;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class OfflineSyncController {

    private final OfflineSyncService syncService;

    @PostMapping("/push")
    public ResponseEntity<PushSyncBatchResult> pushOfflineBatch(@Valid @RequestBody PushSyncBatchRequest request) {
        return ResponseEntity.ok(syncService.processPushSync(request));
    }

    @PostMapping("/pull")
    public ResponseEntity<PullSyncResponse> pullDelta(@Valid @RequestBody PullSyncRequest request) {
        return ResponseEntity.ok(syncService.processPullSync(request));
    }

    @PostMapping("/gate/register")
    @PreAuthorize("hasAuthority('Manage Gate') or hasRole('ADMIN')")
    public ResponseEntity<SignedGateRosterBundle> registerGateDevice(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GateDeviceRegisterRequest request) {
        return ResponseEntity.ok(syncService.registerGateDeviceAndRoster(principal.getCommunityId(), request));
    }

    @GetMapping("/gate/roster")
    @PreAuthorize("hasAuthority('Manage Gate') or hasRole('ADMIN')")
    public ResponseEntity<SignedGateRosterBundle> fetchGateRoster(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String deviceId) {
        return ResponseEntity.ok(syncService.getGateRoster(principal.getCommunityId(), deviceId));
    }

    @GetMapping("/gate/anomalies")
    @PreAuthorize("hasAuthority('Manage Gate') or hasRole('ADMIN')")
    public ResponseEntity<List<GateAnomalyDto>> listGateAnomalies(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(syncService.getUnresolvedGateAnomalies(principal.getCommunityId()));
    }

    @PostMapping("/gate/anomalies/{auditId}/resolve")
    @PreAuthorize("hasAuthority('Manage Gate') or hasRole('ADMIN')")
    public ResponseEntity<GateAnomalyDto> resolveAnomaly(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long auditId,
            @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        return ResponseEntity.ok(syncService.resolveGateAnomaly(
                principal.getCommunityId(), auditId, principal.getUserId(), note));
    }
}
