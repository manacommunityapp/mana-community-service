package com.manacommunity.api.finance.controller;

import com.manacommunity.api.finance.dto.CreateSocietyExpenseDto;
import com.manacommunity.api.finance.service.SocietyFinanceService;
import com.manacommunity.api.service.PermissionCheckService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static com.manacommunity.api.constants.permissions.AdminPermissions.VIEW_ADMIN;

@RestController
@RequestMapping({"/api/api/finance", "/api/api/v1/finance"})
@RequiredArgsConstructor
public class SocietyFinanceAliasController {

    private final SocietyFinanceService service;
    private final LoggedInUserService loggedInUserService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping("/dashboard-summary")
    public ResponseEntity<?> dashboardSummary(@AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getDashboardSummary(user.getCommunity().getId()));
    }

    @GetMapping("/expenses")
    public ResponseEntity<?> listExpenses(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getExpenses(user.getCommunity().getId(), status));
    }

    @PostMapping("/expenses")
    public ResponseEntity<?> createExpense(
            @RequestBody CreateSocietyExpenseDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.createExpense(user.getCommunity().getId(), dto, user));
    }

    @RequestMapping(value = "/expenses/{id}/verify", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<?> verifyExpense(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(service.verifyExpense(id, notes, user));
    }

    @RequestMapping(value = "/expenses/{id}/approve", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<?> approveExpense(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(service.approveExpense(id, notes, user));
    }

    @RequestMapping(value = "/expenses/{id}/disburse", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<?> disburseExpense(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        String utr = body != null ? body.get("utr") : null;
        String method = body != null ? body.get("method") : null;
        return ResponseEntity.ok(service.disburseExpense(id, utr, method));
    }

    @GetMapping("/chart-of-accounts")
    public ResponseEntity<?> chartOfAccounts(@AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getChartOfAccounts(user.getCommunity().getId()));
    }

    @GetMapping("/ledger-entries")
    public ResponseEntity<?> ledgerEntries(@AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getLedgerEntries(user.getCommunity().getId()));
    }

    @GetMapping("/reports/income-expenditure")
    public ResponseEntity<?> incomeExpenditure(
            @RequestParam(required = false) String year,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getIncomeExpenseStatement(user.getCommunity().getId(), year));
    }

    @GetMapping("/reports/balance-sheet")
    public ResponseEntity<?> balanceSheet(@AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getBalanceSheet(user.getCommunity().getId()));
    }

    @GetMapping("/reports/gst-summary")
    public ResponseEntity<?> gstSummary(
            @RequestParam(required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getGstSummary(user.getCommunity().getId(), month));
    }

    @GetMapping("/invoices")
    public ResponseEntity<?> listInvoices(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", List.of());
        result.put("totalElements", 0);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/invoices")
    public ResponseEntity<?> createInvoice(
            @RequestBody Map<String, Object> dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Map<String, Object> invoice = new LinkedHashMap<>();
        invoice.put("id", "inv-" + System.currentTimeMillis());
        invoice.put("invoiceNumber", "INV-" + System.currentTimeMillis());
        invoice.put("unitNumber", dto.getOrDefault("unitNumber", user.getFlatNo()));
        invoice.put("residentName", dto.getOrDefault("residentName", user.getFullName()));
        invoice.put("status", "SENT");
        invoice.put("createdAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/vendors")
    public ResponseEntity<?> listVendors(@AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/vendors")
    public ResponseEntity<?> createVendor(
            @RequestBody Map<String, Object> dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        loggedInUserService.resolve(principal);
        Map<String, Object> vendor = new LinkedHashMap<>();
        vendor.put("id", "ven-" + System.currentTimeMillis());
        vendor.put("vendorName", dto.getOrDefault("vendorName", ""));
        vendor.put("category", dto.getOrDefault("category", ""));
        vendor.put("isActive", true);
        vendor.put("createdAt", LocalDateTime.now().toString());
        return ResponseEntity.ok(vendor);
    }
}
