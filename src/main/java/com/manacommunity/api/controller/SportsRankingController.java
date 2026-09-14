package com.manacommunity.api.controller;

import com.manacommunity.api.dto.SportsPlayerRankingRequest;
import com.manacommunity.api.dto.SportsPlayerRankingResponse;
import com.manacommunity.api.service.SportsPlayerRankingService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.manacommunity.api.constants.permissions.SportsPermissions.*;

@RestController
@RequestMapping("/api/sports/rankings")
@RequiredArgsConstructor
public class SportsRankingController {

    private final SportsPlayerRankingService rankingService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping
    public ResponseEntity<List<SportsPlayerRankingResponse>> list(
            @RequestParam Long sportId,
            @RequestParam Long communityId,
            @RequestParam(defaultValue = "CURRENT") String season,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_PLAYER_POOL);
        return ResponseEntity.ok(rankingService.listBySportAndCommunity(sportId, communityId, season));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SportsPlayerRankingResponse>> listForUser(
            @PathVariable Long userId,
            @RequestParam Long communityId,
            @RequestParam(defaultValue = "CURRENT") String season,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MAIN, VIEW_PLAYER_POOL);
        return ResponseEntity.ok(rankingService.listByUserAndCommunity(userId, communityId, season));
    }

    @PostMapping
    public ResponseEntity<SportsPlayerRankingResponse> upsert(
            @Valid @RequestBody SportsPlayerRankingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN, CREATE_EDIT_PLAYER_POOL);
        return ResponseEntity.status(HttpStatus.CREATED).body(rankingService.upsert(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SportsPlayerRankingResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SportsPlayerRankingRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN, CREATE_EDIT_PLAYER_POOL);
        return ResponseEntity.ok(rankingService.upsert(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, DELETE_SPORTS_MAIN, DELETE_PLAYER_POOL);
        rankingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
