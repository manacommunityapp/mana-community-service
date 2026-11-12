package com.manacommunity.api.sports.controller;

import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.SportsStreamService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.manacommunity.api.constants.permissions.SportsPermissions.*;

@RestController
@RequestMapping("/api/sports/matches/{matchId}/stream")
@RequiredArgsConstructor
public class SportsStreamController {

    private final SportsStreamService streamService;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    /** GET /api/sports/matches/{matchId}/stream — any viewer */
    @GetMapping
    public ResponseEntity<SportsStreamResponse> getStream(
            @PathVariable Long matchId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MENU, VIEW_SPORTS_MAIN);
        return ResponseEntity.ok(streamService.getStream(matchId));
    }

    /** POST /api/sports/matches/{matchId}/stream/start — admin/scorer only */
    @PostMapping("/start")
    public ResponseEntity<SportsStreamResponse> startStream(
            @PathVariable Long matchId,
            @RequestBody(required = false) SportsStreamStartRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        String title = request != null ? request.getTitle() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(streamService.startStream(matchId, title));
    }

    /** PUT /api/sports/matches/{matchId}/stream/end — admin/scorer only */
    @PutMapping("/end")
    public ResponseEntity<SportsStreamResponse> endStream(
            @PathVariable Long matchId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.ok(streamService.endStream(matchId));
    }

    /** POST /api/sports/matches/{matchId}/stream/cameras — admin/scorer only */
    @PostMapping("/cameras")
    public ResponseEntity<SportsStreamResponse> addCamera(
            @PathVariable Long matchId,
            @Valid @RequestBody SportsStreamCameraRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.status(HttpStatus.CREATED).body(streamService.addCamera(matchId, request));
    }

    /** DELETE /api/sports/matches/{matchId}/stream/cameras/{cameraId} — admin/scorer only */
    @DeleteMapping("/cameras/{cameraId}")
    public ResponseEntity<SportsStreamResponse> removeCamera(
            @PathVariable Long matchId,
            @PathVariable Long cameraId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.ok(streamService.removeCamera(matchId, cameraId));
    }

    /** PUT /api/sports/matches/{matchId}/stream/cameras/{cameraId}/active — admin/scorer only */
    @PutMapping("/cameras/{cameraId}/active")
    public ResponseEntity<SportsStreamResponse> switchCamera(
            @PathVariable Long matchId,
            @PathVariable Long cameraId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, CREATE_EDIT_SPORTS_MAIN);
        return ResponseEntity.ok(streamService.switchActiveCamera(matchId, cameraId));
    }

    /** GET /api/sports/matches/{matchId}/stream/chat — any viewer */
    @GetMapping("/chat")
    public ResponseEntity<SportsStreamResponse> getChat(
            @PathVariable Long matchId,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MENU, VIEW_SPORTS_MAIN);
        return ResponseEntity.ok(streamService.getStream(matchId));
    }

    /** POST /api/sports/matches/{matchId}/stream/chat — any viewer */
    @PostMapping("/chat")
    public ResponseEntity<SportsStreamResponse> postMessage(
            @PathVariable Long matchId,
            @Valid @RequestBody SportsStreamChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_SPORTS_MENU, VIEW_SPORTS_MAIN);
        String authorName = principal != null ? principal.getUsername() : "Viewer";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(streamService.postMessage(matchId, request, authorName));
    }
}
