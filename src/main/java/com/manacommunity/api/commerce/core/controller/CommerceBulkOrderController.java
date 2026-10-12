package com.manacommunity.api.commerce.core.controller;

import com.manacommunity.api.commerce.core.dto.CommerceBulkOrderDto;
import com.manacommunity.api.commerce.core.dto.CommerceBulkOrderRequest;
import com.manacommunity.api.commerce.core.service.CommerceBulkOrderService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/commerce/bulk-orders")
@RequiredArgsConstructor
public class CommerceBulkOrderController {

    private final CommerceBulkOrderService bulkOrderService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    public ResponseEntity<CommerceBulkOrderDto> create(
            @RequestBody CommerceBulkOrderRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bulkOrderService.createBulkOrder(user, req));
    }

    @GetMapping("/my")
    public ResponseEntity<List<CommerceBulkOrderDto>> getMy(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bulkOrderService.getMyBulkOrders(user));
    }

    @PostMapping("/{code}/cancel")
    public ResponseEntity<CommerceBulkOrderDto> cancel(
            @PathVariable String code,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bulkOrderService.cancelBulkOrder(user, code));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<List<CommerceBulkOrderDto>> getAdmin(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bulkOrderService.getAdminBulkOrders(user));
    }

    @PatchMapping("/admin/{code}/status")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<CommerceBulkOrderDto> updateStatus(
            @PathVariable String code,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bulkOrderService.updateStatus(user, code, body.get("status")));
    }

    @PatchMapping("/admin/{code}")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<CommerceBulkOrderDto> updateBulkOrder(
            @PathVariable String code,
            @RequestBody CommerceBulkOrderRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(bulkOrderService.updateBulkOrder(user, code, req));
    }
}
