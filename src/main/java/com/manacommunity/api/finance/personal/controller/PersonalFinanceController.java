package com.manacommunity.api.finance.personal.controller;

import com.manacommunity.api.finance.personal.dto.*;
import com.manacommunity.api.finance.personal.service.PersonalFinanceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/personal-finance")
@RequiredArgsConstructor
public class PersonalFinanceController {

    private final PersonalFinanceService financeService;
    private final LoggedInUserService loggedInUserService;

    // ── Summary ─────────────────────────────────────────────────────

    @GetMapping("/summary")
    public ResponseEntity<FinanceSummaryResponse> getSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getSummary(user.getId()));
    }

    // ── Accounts ────────────────────────────────────────────────────

    @GetMapping("/accounts")
    public ResponseEntity<List<AccountResponse>> getAccounts(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getAccounts(user.getId()));
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AccountRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createAccount(user, request));
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<AccountResponse> updateAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AccountRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.updateAccount(user.getId(), id, request));
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        financeService.deleteAccount(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    // ── Transactions ────────────────────────────────────────────────

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long accountId,
            @PageableDefault(size = 20) Pageable pageable) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getTransactions(user.getId(), accountId, pageable));
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransactionRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createTransaction(user, request));
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.updateTransaction(user.getId(), id, request));
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        financeService.deleteTransaction(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    // ── Categories ──────────────────────────────────────────────────

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getCategories(user.getId()));
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CategoryRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createCategory(user, request));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.updateCategory(user.getId(), id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        financeService.deleteCategory(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    // ── Budgets ─────────────────────────────────────────────────────

    @GetMapping("/budgets")
    public ResponseEntity<List<BudgetResponse>> getBudgets(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam int year,
            @RequestParam(required = false) Integer month) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getBudgets(user.getId(), year, month));
    }

    @PostMapping("/budgets")
    public ResponseEntity<BudgetResponse> createBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BudgetRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createBudget(user, request));
    }

    @PutMapping("/budgets/{id}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.updateBudget(user.getId(), id, request));
    }

    @DeleteMapping("/budgets/{id}")
    public ResponseEntity<Void> deleteBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        financeService.deleteBudget(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    // ── Bills ───────────────────────────────────────────────────────

    @GetMapping("/bills")
    public ResponseEntity<List<BillResponse>> getBills(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getBills(user.getId(), status));
    }

    @PostMapping("/bills")
    public ResponseEntity<BillResponse> createBill(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BillRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createBill(user, request));
    }

    @PutMapping("/bills/{id}")
    public ResponseEntity<BillResponse> updateBill(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody BillRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.updateBill(user.getId(), id, request));
    }

    @PostMapping("/bills/{id}/mark-paid")
    public ResponseEntity<BillResponse> markBillPaid(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.markBillPaid(user.getId(), id));
    }

    @DeleteMapping("/bills/{id}")
    public ResponseEntity<Void> deleteBill(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        financeService.deleteBill(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    // ── Recurring Transactions ──────────────────────────────────────

    @GetMapping("/recurring")
    public ResponseEntity<List<RecurringTxnResponse>> getRecurringTxns(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getRecurringTxns(user.getId()));
    }

    @PostMapping("/recurring")
    public ResponseEntity<RecurringTxnResponse> createRecurringTxn(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody RecurringTxnRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(financeService.createRecurringTxn(user, request));
    }

    @DeleteMapping("/recurring/{id}")
    public ResponseEntity<Void> deleteRecurringTxn(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        financeService.deleteRecurringTxn(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
