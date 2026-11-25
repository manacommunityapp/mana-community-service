package com.manacommunity.api.pins.controller;

import com.manacommunity.api.pins.dto.CommunityPinDto;
import com.manacommunity.api.pins.dto.CreatePinRequest;
import com.manacommunity.api.pins.service.CommunityPinService;
import com.manacommunity.api.platform.web.ApiResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Community Pins", description = "Ephemeral hyperlocal micro-broadcasts and alerts with auto-expiry")
@Slf4j
@RestController
@RequestMapping("/api/communities/{communityId}/pins")
@RequiredArgsConstructor
public class CommunityPinController {

    private final CommunityPinService pinService;
    private final LoggedInUserService loggedInUserService;

    @Operation(summary = "Get active community pins", description = "Returns active non-expired pins for the given community")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CommunityPinDto>>> getPins(
            @PathVariable Long communityId,
            @RequestParam(required = false) String category) {
        List<CommunityPinDto> pins = pinService.getActivePins(communityId, category);
        return ResponseEntity.ok(ApiResponse.ok(pins));
    }

    @Operation(summary = "Create a community pin", description = "Creates a new ephemeral pin that auto-expires after a duration")
    @PostMapping
    public ResponseEntity<ApiResponse<CommunityPinDto>> createPin(
            @PathVariable Long communityId,
            @Valid @RequestBody CreatePinRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser caller = loggedInUserService.resolve(principal);
        Long callerCommunityId = caller.getCommunity() != null ? caller.getCommunity().getId() : null;
        if (!caller.hasRole("SUPER_ADMIN") && !communityId.equals(callerCommunityId)) {
            throw new org.springframework.security.access.AccessDeniedException("You may only post pins within your own community");
        }
        CommunityPinDto pin = pinService.createPin(communityId, caller, request);
        return ResponseEntity.ok(ApiResponse.ok(pin));
    }

    @Operation(summary = "Close a community pin", description = "Marks a pin as closed early by author or admin")
    @PostMapping("/{pinId}/close")
    public ResponseEntity<ApiResponse<CommunityPinDto>> closePin(
            @PathVariable Long communityId,
            @PathVariable Long pinId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser caller = loggedInUserService.resolve(principal);
        Long callerCommunityId = caller.getCommunity() != null ? caller.getCommunity().getId() : null;
        if (!caller.hasRole("SUPER_ADMIN") && !communityId.equals(callerCommunityId)) {
            throw new org.springframework.security.access.AccessDeniedException("You may only manage pins within your own community");
        }
        CommunityPinDto pin = pinService.closePin(pinId, communityId, caller);
        return ResponseEntity.ok(ApiResponse.ok(pin));
    }

    @Operation(summary = "Like a community pin", description = "Increments like count for the pin")
    @PostMapping("/{pinId}/like")
    public ResponseEntity<ApiResponse<CommunityPinDto>> likePin(
            @PathVariable Long communityId,
            @PathVariable Long pinId) {
        CommunityPinDto pin = pinService.toggleLike(pinId, communityId);
        return ResponseEntity.ok(ApiResponse.ok(pin));
    }
}
