package com.manacommunity.api.finance.personal.controller;

import com.manacommunity.api.finance.personal.dto.*;
import com.manacommunity.api.finance.personal.service.PersonalFinanceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Controller for the resident-level Personal Finance / Money Manager module.
 *
 * <p>Supports dual routing paths (/api/v1/personal-finance/* and /personal-finance/*)
 * to accommodate both modern and direct mobile API client requests.
 *
 * <p>Security rule: All operations are strictly scoped to the authenticated
 * resident's private data. No community admin or third party can access this data.
 */
@RestController
@RequestMapping
@RequiredArgsConstructor
@Slf4j
public class PersonalFinanceController {

    private final PersonalFinanceService financeService;
    private final LoggedInUserService loggedInUserService;

    // ─── Accounts ───────────────────────────────────────────────────────────────

    @GetMapping({"/api/v1/personal-finance/accounts", "/personal-finance/accounts"})
    public ResponseEntity<List<PersonalAccountDto>> getAccounts(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getAccounts(user));
    }

    @PostMapping({"/api/v1/personal-finance/accounts", "/personal-finance/accounts"})
    public ResponseEntity<PersonalAccountDto> createAccount(
            @Valid @RequestBody CreatePersonalAccountDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.createAccount(dto, user));
    }

    @PutMapping({"/api/v1/personal-finance/accounts/{id}", "/personal-finance/accounts/{id}"})
    public ResponseEntity<PersonalAccountDto> updateAccount(
            @PathVariable("id") String id,
            @Valid @RequestBody CreatePersonalAccountDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.updateAccount(id, dto, user));
    }

    // ─── Categories ─────────────────────────────────────────────────────────────

    @GetMapping({"/api/v1/personal-finance/categories", "/personal-finance/categories"})
    public ResponseEntity<List<PersonalCategoryDto>> getCategories(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getCategories(user));
    }

    @PostMapping({"/api/v1/personal-finance/categories", "/personal-finance/categories"})
    public ResponseEntity<PersonalCategoryDto> createCategory(
            @Valid @RequestBody CreatePersonalCategoryDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.createCategory(dto, user));
    }

    // ─── Transactions ───────────────────────────────────────────────────────────

    @GetMapping({"/api/v1/personal-finance/transactions", "/personal-finance/transactions"})
    public ResponseEntity<List<PersonalTransactionDto>> getTransactions(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "accountId", required = false) String accountId,
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "limit", defaultValue = "50") int limit,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getTransactions(user, type, categoryId, accountId, from, to, tag, page, limit));
    }

    @GetMapping({"/api/v1/personal-finance/transactions/{id}", "/personal-finance/transactions/{id}"})
    public ResponseEntity<PersonalTransactionDto> getTransaction(
            @PathVariable("id") String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getTransaction(id, user));
    }

    @PostMapping({"/api/v1/personal-finance/transactions", "/personal-finance/transactions"})
    public ResponseEntity<PersonalTransactionDto> createTransaction(
            @Valid @RequestBody CreatePersonalTransactionDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.createTransaction(dto, user));
    }

    @PostMapping({"/api/v1/personal-finance/transactions/batch-import", "/personal-finance/transactions/batch-import"})
    public ResponseEntity<BatchImportResultDto> batchImport(
            @Valid @RequestBody BatchImportTransactionDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.batchImportTransactions(dto, user));
    }

    @DeleteMapping({"/api/v1/personal-finance/transactions/{id}", "/personal-finance/transactions/{id}"})
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable("id") String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        financeService.deleteTransaction(id, user);
        return ResponseEntity.noContent().build();
    }

    
    @PostMapping({"/api/v1/personal-finance/transactions/parse-text", "/personal-finance/transactions/parse-text"})
    public ResponseEntity<CreatePersonalTransactionDto> parseNaturalLanguageText(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String text = body != null ? body.get("text") : "";
        return ResponseEntity.ok(financeService.parseNaturalLanguageText(text, user));
    }

    // ─── Dashboard Summary ──────────────────────────────────────────────────────

    @GetMapping({"/api/v1/personal-finance/dashboard", "/personal-finance/dashboard"})
    public ResponseEntity<PersonalDashboardSummaryDto> getDashboard(
            @RequestParam(value = "month", required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getDashboardSummary(user, month));
    }

    // ─── Budgets ────────────────────────────────────────────────────────────────

    @GetMapping({"/api/v1/personal-finance/budgets", "/personal-finance/budgets"})
    public ResponseEntity<List<PersonalBudgetDto>> getBudgets(
            @RequestParam(value = "month", required = false) String month,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getBudgets(user, month));
    }

    @PostMapping({"/api/v1/personal-finance/budgets", "/personal-finance/budgets"})
    public ResponseEntity<PersonalBudgetDto> createBudget(
            @Valid @RequestBody CreatePersonalBudgetDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.createBudget(dto, user));
    }

    // ─── Recurring Rules ────────────────────────────────────────────────────────

    @GetMapping({"/api/v1/personal-finance/recurring", "/personal-finance/recurring"})
    public ResponseEntity<List<PersonalRecurringDto>> getRecurring(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getRecurringTransactions(user));
    }

    @PostMapping({"/api/v1/personal-finance/recurring", "/personal-finance/recurring"})
    public ResponseEntity<PersonalRecurringDto> createRecurring(
            @Valid @RequestBody CreatePersonalRecurringDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.createRecurring(dto, user));
    }

    @PostMapping({"/api/v1/personal-finance/recurring/{id}/toggle", "/personal-finance/recurring/{id}/toggle"})
    public ResponseEntity<PersonalRecurringDto> toggleRecurring(
            @PathVariable("id") String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.toggleRecurring(id, user));
    }

    @PostMapping({"/api/v1/personal-finance/recurring/process-due", "/personal-finance/recurring/process-due"})
    public ResponseEntity<Map<String, Object>> processDueRecurring(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        int count = financeService.processDueRecurring(user);
        return ResponseEntity.ok(Map.of("processedCount", count, "message", "Processed " + count + " recurring transactions"));
    }

    // ─── Bills ──────────────────────────────────────────────────────────────────

    @GetMapping({"/api/v1/personal-finance/bills", "/personal-finance/bills"})
    public ResponseEntity<List<PersonalBillDto>> getBills(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getBills(user));
    }

    @PostMapping({"/api/v1/personal-finance/bills/{id}/mark-paid", "/personal-finance/bills/{id}/mark-paid"})
    public ResponseEntity<Void> markBillPaid(
            @PathVariable("id") String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        financeService.markBillPaid(id, user);
        return ResponseEntity.ok().build();
    }

    // ─── Installments & Loans (P3) ──────────────────────────────────────────────

    @GetMapping({"/api/v1/personal-finance/installments", "/personal-finance/installments"})
    public ResponseEntity<List<PersonalInstallmentDto>> getInstallments(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getInstallments(user));
    }

    @PostMapping({"/api/v1/personal-finance/installments", "/personal-finance/installments"})
    public ResponseEntity<PersonalInstallmentDto> createInstallment(
            @Valid @RequestBody CreatePersonalInstallmentDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.createInstallment(dto, user));
    }

    @PostMapping({"/api/v1/personal-finance/installments/{id}/pay", "/personal-finance/installments/{id}/pay"})
    public ResponseEntity<PersonalInstallmentDto> payInstallment(
            @PathVariable("id") String id,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        BigDecimal amount = body != null && body.get("amount") != null ? new BigDecimal(body.get("amount").toString()) : null;
        String accountId = body != null && body.get("accountId") != null ? body.get("accountId").toString() : null;
        return ResponseEntity.ok(financeService.payInstallment(id, amount, accountId, user));
    }

    @DeleteMapping({"/api/v1/personal-finance/installments/{id}", "/personal-finance/installments/{id}"})
    public ResponseEntity<Void> deleteInstallment(
            @PathVariable("id") String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        financeService.deleteInstallment(id, user);
        return ResponseEntity.noContent().build();
    }

    // ─── Savings Goals (P3) ─────────────────────────────────────────────────────

    @GetMapping({"/api/v1/personal-finance/goals", "/personal-finance/goals"})
    public ResponseEntity<List<PersonalGoalDto>> getGoals(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getGoals(user));
    }

    @PostMapping({"/api/v1/personal-finance/goals", "/personal-finance/goals"})
    public ResponseEntity<PersonalGoalDto> createGoal(
            @Valid @RequestBody CreatePersonalGoalDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.createGoal(dto, user));
    }

    @PostMapping({"/api/v1/personal-finance/goals/{id}/contribute", "/personal-finance/goals/{id}/contribute"})
    public ResponseEntity<PersonalGoalDto> contributeToGoal(
            @PathVariable("id") String id,
            @Valid @RequestBody GoalContributionDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.contributeToGoal(id, dto, user));
    }

    @DeleteMapping({"/api/v1/personal-finance/goals/{id}", "/personal-finance/goals/{id}"})
    public ResponseEntity<Void> deleteGoal(
            @PathVariable("id") String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        financeService.deleteGoal(id, user);
        return ResponseEntity.noContent().build();
    }

    // ─── Analytics & Reports ────────────────────────────────────────────────────

    @GetMapping({"/api/v1/personal-finance/reports", "/personal-finance/reports"})
    public ResponseEntity<PersonalReportPeriodDto> getReport(
            @RequestParam(value = "period", defaultValue = "this-month") String period,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getReport(user, period));
    }

    // ─── Mana Projections ───────────────────────────────────────────────────────

    @GetMapping({"/api/v1/personal-finance/mana-projections", "/personal-finance/mana-projections"})
    public ResponseEntity<List<PersonalTransactionDto>> getManaProjections(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(financeService.getManaProjections(user));
    }
}
