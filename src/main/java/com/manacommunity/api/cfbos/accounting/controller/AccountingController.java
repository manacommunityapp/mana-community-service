package com.manacommunity.api.cfbos.accounting.controller;

import com.manacommunity.api.cfbos.accounting.dto.*;
import com.manacommunity.api.cfbos.accounting.engine.AccountingEngine;
import com.manacommunity.api.cfbos.accounting.entity.JournalEntry;
import com.manacommunity.api.cfbos.accounting.service.*;
import com.manacommunity.api.constants.PermissionConstants;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.PermissionCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cfbos/v1/accounting")
@RequiredArgsConstructor
public class AccountingController {

    private final AccountingEngine accountingEngine;
    private final ChartOfAccountsService chartOfAccountsService;
    private final JournalEntryService journalEntryService;
    private final FiscalYearService fiscalYearService;
    private final TrialBalanceService trialBalanceService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountTreeNodeDto>> getAccountTree(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, PermissionConstants.VIEW_ADMIN);
        return ResponseEntity.ok(chartOfAccountsService.getAccountTree());
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountDto> createAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody AccountDto dto) {
        permissionCheckService.requireAnyPermission(principal, PermissionConstants.VIEW_ADMIN);
        return ResponseEntity.ok(chartOfAccountsService.createAccount(dto));
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountDto> getAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        permissionCheckService.requireAnyPermission(principal, PermissionConstants.VIEW_ADMIN);
        return ResponseEntity.ok(chartOfAccountsService.getAccount(id));
    }

    @PostMapping("/journal-entries")
    public ResponseEntity<JournalEntryDto> createJournalEntry(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody JournalEntryRequest request) {
        permissionCheckService.requireAnyPermission(principal, PermissionConstants.VIEW_ADMIN);
        JournalEntry entry = accountingEngine.createAndPostJournalEntry(request);
        return ResponseEntity.ok(journalEntryService.getById(entry.getId()));
    }

    @GetMapping("/journal-entries")
    public ResponseEntity<Page<JournalEntryDto>> listJournalEntries(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        permissionCheckService.requireAnyPermission(principal, PermissionConstants.VIEW_ADMIN);
        return ResponseEntity.ok(journalEntryService.list(from, to, pageable));
    }

    @GetMapping("/journal-entries/{id}")
    public ResponseEntity<JournalEntryDto> getJournalEntry(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        permissionCheckService.requireAnyPermission(principal, PermissionConstants.VIEW_ADMIN);
        return ResponseEntity.ok(journalEntryService.getById(id));
    }

    @PostMapping("/journal-entries/{id}/reverse")
    public ResponseEntity<JournalEntryDto> reverseJournalEntry(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam String reason) {
        permissionCheckService.requireAnyPermission(principal, PermissionConstants.VIEW_ADMIN);
        JournalEntry reversal = accountingEngine.reverseJournalEntry(id, reason);
        return ResponseEntity.ok(journalEntryService.getById(reversal.getId()));
    }

    @GetMapping("/fiscal-years")
    public ResponseEntity<List<FiscalYearDto>> getFiscalYears(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, PermissionConstants.VIEW_ADMIN);
        return ResponseEntity.ok(fiscalYearService.getAll());
    }

    @PostMapping("/fiscal-years")
    public ResponseEntity<FiscalYearDto> createFiscalYear(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody FiscalYearDto dto) {
        permissionCheckService.requireAnyPermission(principal, PermissionConstants.VIEW_ADMIN);
        return ResponseEntity.ok(fiscalYearService.create(dto));
    }

    @GetMapping("/trial-balance")
    public ResponseEntity<TrialBalanceDto> getTrialBalance(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam Long fiscalYearId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {
        permissionCheckService.requireAnyPermission(principal, PermissionConstants.VIEW_ADMIN);
        return ResponseEntity.ok(trialBalanceService.generate(fiscalYearId, asOfDate));
    }
}
