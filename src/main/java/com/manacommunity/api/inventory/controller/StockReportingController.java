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
@RequestMapping("/api/inventory/reporting")
@RequiredArgsConstructor
public class StockReportingController {

    private final StockReportingService reportingService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping("/stock")
    public ResponseEntity<List<StockLevelReportDto>> getStockLevelReport(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(reportingService.getStockLevelReport());
    }

    @GetMapping("/moves-history")
    public ResponseEntity<List<MoveHistoryReportDto>> getMoveHistoryReport(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(reportingService.getMoveHistoryReport());
    }
}
