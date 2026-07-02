package com.manacommunity.api.inventory.controller;

import com.manacommunity.api.inventory.dto.*;
import com.manacommunity.api.inventory.service.*;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.PermissionCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import static com.manacommunity.api.constants.PermissionConstants.VIEW_ADMIN;

@RestController
@RequestMapping("/api/inventory/lots")
@RequiredArgsConstructor
public class StockLotController {

    private final StockQuantService quantService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping
    public ResponseEntity<List<LotDto>> getAllLots(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(quantService.getAllLots());
    }

    @PostMapping
    public ResponseEntity<LotDto> createLot(
            @RequestBody LotDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(quantService.createLot(dto));
    }
}
