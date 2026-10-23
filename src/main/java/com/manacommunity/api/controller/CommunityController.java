package com.manacommunity.api.controller;

import com.manacommunity.api.dto.BlockConfigRequest;
import com.manacommunity.api.dto.BlockConfigResponse;
import com.manacommunity.api.model.Community;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.response.CommunityResponse;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.CommunityBlockConfigService;
import com.manacommunity.api.service.CommunityService;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Community endpoints.
 * GET is public (signup dropdown). POST requires authentication.
 */
@RestController
@RequestMapping("/api/communities")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    @Autowired
    private CommunityBlockConfigService blockConfigService;

    @Autowired
    private LoggedInUserService loggedInUserService;

    /**
     * GET /api/communities
     * Returns all communities for the signup dropdown.
     * Optional ?type= filter (APARTMENT | COLLEGE | SCHOOL | OFFICE)
     *
     * Example:
     *   GET /api/communities          → all communities
     *   GET /api/communities?type=APARTMENT → only apartments
     */
    @GetMapping
    public ResponseEntity<List<CommunityResponse>> getCommunities(
            @RequestParam(required = false) String type) {

        List<CommunityResponse> result = (type != null && !type.isBlank())
                ? communityService.getCommunitiesByType(type)
                : communityService.getAllCommunities();

        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/communities
     * Creates a new community. Restricted to ADMIN and SUPER_ADMIN.
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<CommunityResponse> createCommunity(
            @Valid @RequestBody CommunityResponse request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(communityService.createCommunity(request));
    }

    /**
     * PUT /api/communities/{id}
     * Updates an existing community. Restricted to ADMIN and SUPER_ADMIN.
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<CommunityResponse> updateCommunity(
            @PathVariable Long id,
            @Valid @RequestBody CommunityResponse request,
            @AuthenticationPrincipal UserPrincipal principal) {
        assertCommunityOwnership(id, principal);
        return ResponseEntity.ok(communityService.updateCommunity(id, request));
    }

    /**
     * DELETE /api/communities/{id}
     * Soft-deletes a community (sets active=false). Restricted to ADMIN and SUPER_ADMIN.
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommunity(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        assertCommunityOwnership(id, principal);
        communityService.deleteCommunity(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/communities/{id}/modules
     * Updates enabled feature modules for a community. SUPER_ADMIN only.
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @PutMapping("/{id}/modules")
    public ResponseEntity<CommunityResponse> updateModules(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, java.util.List<String>> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(communityService.updateEnabledModules(id, body.get("modules")));
    }

    /**
     * GET /api/communities/{id}/blocks
     * Returns the block → floor → flat hierarchy for the given community.
     * Used by the signup / profile forms to populate cascading dropdowns.
     * Public — no authentication required.
     *
     * Example response:
     * [
     *   { "blockName": "A", "totalFloors": 10, "flatsPerFloor": 11, "totalFlats": 110,
     *     "floors": [ { "floor": 1, "flats": ["101","102",...,"111"] }, ... ] },
     *   ...
     * ]
     */
    @GetMapping("/{id}/blocks")
    public ResponseEntity<List<BlockConfigResponse>> getBlockConfigs(@PathVariable Long id) {
        return ResponseEntity.ok(blockConfigService.getBlockConfigs(id));
    }

    /**
     * POST /api/communities/{id}/blocks
     * Admin: create or update a block configuration entry.
     * Restricted to ADMIN and SUPER_ADMIN.
     */
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping("/{id}/blocks")
    public ResponseEntity<BlockConfigResponse> saveBlockConfig(
            @PathVariable Long id,
            @Valid @RequestBody BlockConfigRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        assertCommunityOwnership(id, principal);
        return ResponseEntity.ok(blockConfigService.saveBlockConfig(id, request));
    }

    // SUPER_ADMIN may act on any community; ADMIN is restricted to their own.
    private void assertCommunityOwnership(Long communityId, UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.hasRole("SUPER_ADMIN")) return;
        Long callerCommunityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (!communityId.equals(callerCommunityId)) {
            throw new AccessDeniedException("You may only modify your own community.");
        }
    }

    /**
     * GET /api/communities/check-unit
     * Public check to verify if a Block/Wing and Flat Number is already registered in a community.
     */
    @GetMapping("/check-unit")
    public ResponseEntity<java.util.Map<String, Object>> checkUnitExists(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String inviteCode,
            @RequestParam String block,
            @RequestParam String flatNo) {
        boolean exists = communityService.checkUnitExists(communityId, inviteCode, block, flatNo);
        return ResponseEntity.ok(java.util.Map.of("exists", exists));
    }
}
