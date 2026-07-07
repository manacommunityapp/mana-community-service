package com.manacommunity.api.finance.controller;

import com.manacommunity.api.finance.dto.FinanceBusinessExpenseDto;
import com.manacommunity.api.finance.service.FinanceBusinessExpenseService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.manacommunity.api.constants.PermissionConstants.VIEW_ADMIN;

/** Dedicated REST API for the Expense → Business Expenses menu. */
@RestController
@RequestMapping("/api/finance/business-expenses")
@RequiredArgsConstructor
public class FinanceBusinessExpenseController {

    private final FinanceBusinessExpenseService service;
    private final PermissionCheckService permissionCheckService;

    @GetMapping
    public ResponseEntity<List<FinanceBusinessExpenseDto>> getAll(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.getAll(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinanceBusinessExpenseDto> get(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    public ResponseEntity<FinanceBusinessExpenseDto> create(@RequestBody FinanceBusinessExpenseDto body,
                                                            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(service.create(body));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FinanceBusinessExpenseDto> update(@PathVariable Long id, @RequestBody FinanceBusinessExpenseDto body,
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
