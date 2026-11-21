package com.manacommunity.api.vendor.master.controller;

import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.vendor.master.dto.CreateManaVendorRequest;
import com.manacommunity.api.vendor.master.dto.ManaVendorDto;
import com.manacommunity.api.vendor.master.dto.UpdateManaVendorRequest;
import com.manacommunity.api.vendor.master.service.ManaVendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.manacommunity.api.constants.permissions.AdminPermissions.VIEW_ADMIN;

/**
 * Unified vendor master API.
 *
 * All endpoints are community-scoped. The existing /api/finance/vendors and
 * /api/asset-finance/vendors endpoints remain available for backward compatibility
 * but new code should use this endpoint.
 */
@RestController
@RequestMapping("/api/vendors/master")
@RequiredArgsConstructor
public class ManaVendorController {

    private final ManaVendorService service;
    private final PermissionCheckService permissionCheckService;

    /** List all vendors for a community (active + inactive). */
    @GetMapping
    public ResponseEntity<List<ManaVendorDto>> list(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.listByCommunity(communityId));
    }

    /** List only active vendors — suitable for picker/autocomplete dropdowns. */
    @GetMapping("/active")
    public ResponseEntity<List<ManaVendorDto>> listActive(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.listActiveByCommunity(communityId));
    }

    /** Search vendors by name or email — for autocomplete in forms. */
    @GetMapping("/search")
    public ResponseEntity<List<ManaVendorDto>> search(
            @RequestParam(defaultValue = "") String q,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.search(communityId, q));
    }

    /** Paginated vendor list (for the management table). */
    @GetMapping("/page")
    public ResponseEntity<Page<ManaVendorDto>> page(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.pageByCommunity(communityId, page, size));
    }

    /** Get a single vendor by id. */
    @GetMapping("/{id}")
    public ResponseEntity<ManaVendorDto> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.getById(communityId, id));
    }

    /** Create a new vendor in the unified master. */
    @PostMapping
    public ResponseEntity<ManaVendorDto> create(
            @Valid @RequestBody CreateManaVendorRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.create(request));
    }

    /** Update a vendor (partial update — only non-null fields are applied). */
    @PatchMapping("/{id}")
    public ResponseEntity<ManaVendorDto> update(
            @PathVariable Long id,
            @RequestBody UpdateManaVendorRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.update(communityId, id, request));
    }

    /**
     * Deactivate a vendor.
     * Vendors are never hard-deleted — financial history must be preserved.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal.getCommunityId();
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        service.deactivate(communityId, id);
        return ResponseEntity.noContent().build();
    }
}
