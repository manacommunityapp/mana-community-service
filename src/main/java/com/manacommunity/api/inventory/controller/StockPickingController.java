package com.manacommunity.api.inventory.controller;

import com.manacommunity.api.inventory.dto.*;
import com.manacommunity.api.inventory.service.*;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.PermissionCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import static com.manacommunity.api.constants.PermissionConstants.VIEW_ADMIN;

@RestController
@RequestMapping("/api/inventory/pickings")
@RequiredArgsConstructor
public class StockPickingController {

    private final StockTransferService transferService;
    private final StockQuantService quantService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping
    public ResponseEntity<List<PickingDto>> getAllPickings(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(transferService.getAllPickings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PickingDto> getPickingById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(transferService.getPickingById(id));
    }

    @PostMapping
    public ResponseEntity<PickingDto> createPicking(
            @RequestBody PickingCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(transferService.createPicking(request));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<PickingDto> confirmPicking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(transferService.confirmPicking(id));
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<PickingDto> validatePicking(
            @PathVariable Long id,
            @RequestBody(required = false) List<PickingCreateRequest.LineRequest> lines,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(transferService.validatePicking(id, lines));
    }

    @PostMapping("/scrap")
    public ResponseEntity<Void> scrapStock(
            @RequestParam Long productId,
            @RequestParam Long locationId,
            @RequestParam(required = false) Long lotId,
            @RequestParam Double quantity,
            @RequestParam String reason,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        quantService.scrapStock(productId, locationId, lotId, quantity, reason);
        return ResponseEntity.ok().build();
    }
}
