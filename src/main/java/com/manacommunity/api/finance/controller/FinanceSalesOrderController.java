package com.manacommunity.api.finance.controller;

import com.manacommunity.api.finance.dto.FinanceSalesOrderDto;
import com.manacommunity.api.finance.service.FinanceSalesOrderService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.manacommunity.api.constants.PermissionConstants.VIEW_ADMIN;

/** Dedicated REST API for the Income → Sales Orders menu. */
@RestController
@RequestMapping("/api/finance/sales-orders")
@RequiredArgsConstructor
public class FinanceSalesOrderController {

    private final FinanceSalesOrderService service;
    private final PermissionCheckService permissionCheckService;

    @GetMapping
    public ResponseEntity<List<FinanceSalesOrderDto>> getAll(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.getAll(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinanceSalesOrderDto> get(@PathVariable Long id,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    public ResponseEntity<FinanceSalesOrderDto> create(@RequestBody FinanceSalesOrderDto body,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.create(body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FinanceSalesOrderDto> update(@PathVariable Long id, @RequestBody FinanceSalesOrderDto body,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.update(id, body));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
