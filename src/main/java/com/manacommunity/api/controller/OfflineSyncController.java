package com.manacommunity.api.controller;

import com.manacommunity.api.dto.SyncPullResponse;
import com.manacommunity.api.dto.SyncPushRequest;
import com.manacommunity.api.dto.SyncPushResponse;
import com.manacommunity.api.service.OfflineSyncService;
import com.manacommunity.api.user.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sync")
public class OfflineSyncController {

    @Autowired
    private OfflineSyncService syncService;

    @PostMapping("/push")
    public ResponseEntity<SyncPushResponse> pushMutations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody SyncPushRequest request) {
        return ResponseEntity.ok(syncService.pushMutations(principal.getId(), request));
    }

    @GetMapping("/pull")
    public ResponseEntity<SyncPullResponse> pullChanges(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long sinceCheckpoint,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(syncService.pullChanges(principal.getCommunityId(), sinceCheckpoint, limit));
    }
}
