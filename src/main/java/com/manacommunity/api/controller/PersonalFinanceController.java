package com.manacommunity.api.controller;

import com.manacommunity.api.dto.PersonalBudgetRequest;
import com.manacommunity.api.dto.PersonalGoalRequest;
import com.manacommunity.api.dto.PersonalTransactionRequest;
import com.manacommunity.api.model.PersonalBudget;
import com.manacommunity.api.model.PersonalGoal;
import com.manacommunity.api.model.PersonalTransaction;
import com.manacommunity.api.service.PersonalFinanceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/personal-finance")
@RequiredArgsConstructor
public class PersonalFinanceController {

    private final PersonalFinanceService personalFinanceService;
    private final LoggedInUserService loggedInUserService;

    // ── Transactions ─────────────────────────────────────────────────────

    @GetMapping("/transactions")
    public ResponseEntity<List<PersonalTransaction>> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(personalFinanceService.getTransactions(user, type, category, from, to));
    }

    @PostMapping("/transactions")
    public ResponseEntity<PersonalTransaction> createTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PersonalTransactionRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(personalFinanceService.createTransaction(user, request));
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<PersonalTransaction> updateTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody PersonalTransactionRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(personalFinanceService.updateTransaction(user, id, request));
    }

    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        personalFinanceService.deleteTransaction(user, id);
        return ResponseEntity.noContent().build();
    }

    // ── Budgets ──────────────────────────────────────────────────────────

    @GetMapping("/budgets")
    public ResponseEntity<List<PersonalBudget>> getBudgets(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(personalFinanceService.getBudgets(user, month, year));
    }

    @PostMapping("/budgets")
    public ResponseEntity<PersonalBudget> createBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PersonalBudgetRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(personalFinanceService.createBudget(user, request));
    }

    @PutMapping("/budgets/{id}")
    public ResponseEntity<PersonalBudget> updateBudget(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody PersonalBudgetRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(personalFinanceService.updateBudget(user, id, request));
    }

    // ── Goals ────────────────────────────────────────────────────────────

    @GetMapping("/goals")
    public ResponseEntity<List<PersonalGoal>> getGoals(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(personalFinanceService.getGoals(user));
    }

    @PostMapping("/goals")
    public ResponseEntity<PersonalGoal> createGoal(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PersonalGoalRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(personalFinanceService.createGoal(user, request));
    }

    @PutMapping("/goals/{id}")
    public ResponseEntity<PersonalGoal> updateGoal(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody PersonalGoalRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(personalFinanceService.updateGoal(user, id, request));
    }

    @PutMapping("/goals/{id}/contribute")
    public ResponseEntity<PersonalGoal> contributeToGoal(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam BigDecimal amount) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(personalFinanceService.contributeToGoal(user, id, amount));
    }

    // ── Summary ──────────────────────────────────────────────────────────

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getMonthlySummary(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        AppUser user = loggedInUserService.resolve(principal);
        LocalDate now = LocalDate.now();
        int m = month != null ? month : now.getMonthValue();
        int y = year != null ? year : now.getYear();
        return ResponseEntity.ok(personalFinanceService.getMonthlySummary(user, m, y));
    }
}
