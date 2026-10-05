package com.manacommunity.api.finance.controller;

import com.manacommunity.api.finance.dto.*;
import com.manacommunity.api.finance.service.SocietyFinanceService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.manacommunity.api.constants.permissions.AdminPermissions.VIEW_ADMIN;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class SocietyFinanceController {

    private final SocietyFinanceService service;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping("/dashboard-summary")
    public ResponseEntity<SocietyDashboardSummaryDto> dashboardSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getDashboardSummary(user.getCommunity().getId()));
    }

    @GetMapping("/expenses")
    public ResponseEntity<List<SocietyExpenseDto>> listExpenses(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getExpenses(user.getCommunity().getId(), status));
    }

    @PostMapping("/expenses")
    public ResponseEntity<SocietyExpenseDto> createExpense(
            @Valid @RequestBody CreateSocietyExpenseDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.createExpense(user.getCommunity().getId(), dto, user));
    }

    @PostMapping("/expenses/{id}/verify")
    public ResponseEntity<SocietyExpenseDto> verifyExpense(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(service.verifyExpense(id, notes, user));
    }

    @PostMapping("/expenses/{id}/approve")
    public ResponseEntity<SocietyExpenseDto> approveExpense(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(service.approveExpense(id, notes, user));
    }

    @PostMapping("/expenses/{id}/disburse")
    public ResponseEntity<SocietyExpenseDto> disburseExpense(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        String utr = body != null ? body.get("utrReference") : null;
        String method = body != null ? body.get("paymentMethod") : null;
        return ResponseEntity.ok(service.disburseExpense(id, utr, method));
    }

    @GetMapping("/chart-of-accounts")
    public ResponseEntity<List<Map<String, Object>>> chartOfAccounts(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getChartOfAccounts(user.getCommunity().getId()));
    }

    @GetMapping("/ledger-entries")
    public ResponseEntity<List<Map<String, Object>>> ledgerEntries(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getLedgerEntries(user.getCommunity().getId()));
    }

    @GetMapping("/reports/income-expenditure")
    public ResponseEntity<Map<String, Object>> incomeExpenditure(
            @RequestParam(required = false) String year,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getIncomeExpenseStatement(user.getCommunity().getId(), year));
    }

    @GetMapping("/reports/balance-sheet")
    public ResponseEntity<Map<String, Object>> balanceSheet(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getBalanceSheet(user.getCommunity().getId()));
    }

    @GetMapping("/reports/gst-summary")
    public ResponseEntity<Map<String, Object>> gstSummary(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getGstSummary(user.getCommunity().getId(), month));
    }
}
