package com.manacommunity.api.controller;

import com.manacommunity.api.dto.SocietyDueRequest;
import com.manacommunity.api.dto.SocietyExpenseRequest;
import com.manacommunity.api.model.SocietyDue;
import com.manacommunity.api.model.SocietyExpenseRecord;
import com.manacommunity.api.service.SocietyFinanceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/society-finance")
@RequiredArgsConstructor
public class SocietyFinanceController {

    private final SocietyFinanceService societyFinanceService;
    private final LoggedInUserService loggedInUserService;

    // ── Dues ─────────────────────────────────────────────────────────────

    @GetMapping("/dues")
    public ResponseEntity<List<SocietyDue>> getDues(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String flat) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(societyFinanceService.getDues(communityId, status, flat));
    }

    @PostMapping("/dues")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SocietyDue> createDue(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SocietyDueRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(societyFinanceService.createDue(communityId, request));
    }

    @PutMapping("/dues/{id}/pay")
    public ResponseEntity<SocietyDue> markDuePaid(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) String receiptUrl) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(societyFinanceService.markDuePaid(communityId, id, receiptUrl));
    }

    @GetMapping("/dues/summary")
    public ResponseEntity<Map<String, Object>> getDuesSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(societyFinanceService.getDuesSummary(communityId));
    }

    // ── Expenses ─────────────────────────────────────────────────────────

    @GetMapping("/expenses")
    public ResponseEntity<List<SocietyExpenseRecord>> getExpenses(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(societyFinanceService.getExpenses(communityId, category, from, to));
    }

    @PostMapping("/expenses")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ResponseEntity<SocietyExpenseRecord> createExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SocietyExpenseRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(societyFinanceService.createExpense(communityId, request));
    }

    @GetMapping("/expenses/summary")
    public ResponseEntity<Map<String, Object>> getExpensesSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(societyFinanceService.getExpensesSummary(communityId));
    }

    // ── Balance Sheet ────────────────────────────────────────────────────

    @GetMapping("/balance-sheet")
    public ResponseEntity<Map<String, Object>> getBalanceSheet(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(societyFinanceService.getBalanceSheet(communityId, month, year));
    }
}
