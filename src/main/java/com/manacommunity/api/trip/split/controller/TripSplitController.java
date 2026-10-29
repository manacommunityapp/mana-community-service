package com.manacommunity.api.trip.split.controller;

import com.manacommunity.api.exception.UnauthorizedActionException;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.BudgetRequest;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.DashboardView;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.ExpenseRequest;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.ExpenseView;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.PaymentRequest;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.PaymentView;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.PrefRequest;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.PrefView;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.SummaryView;
import com.manacommunity.api.trip.split.dto.TripSplitDtos.TransferView;
import com.manacommunity.api.trip.split.service.TripBudgetService;
import com.manacommunity.api.trip.split.service.TripExpenseService;
import com.manacommunity.api.trip.split.service.TripMyMoneyPrefService;
import com.manacommunity.api.trip.split.service.TripSettlementService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trips/{tripId}/split")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TripSplitController {

    private final TripExpenseService expenseService;
    private final TripSettlementService settlementService;
    private final TripBudgetService budgetService;
    private final TripMyMoneyPrefService prefService;

    @GetMapping("/expenses")
    public ResponseEntity<List<ExpenseView>> listExpenses(
            @PathVariable String tripId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(expenseService.list(tripId, actorId(principal)));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ExpenseView> createExpense(
            @PathVariable String tripId,
            @RequestBody ExpenseRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(expenseService.create(tripId, req, actorId(principal)));
    }

    @GetMapping("/expenses/{id}")
    public ResponseEntity<ExpenseView> getExpense(
            @PathVariable String tripId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(expenseService.get(tripId, id, actorId(principal)));
    }

    @PutMapping("/expenses/{id}")
    public ResponseEntity<ExpenseView> updateExpense(
            @PathVariable String tripId,
            @PathVariable Long id,
            @RequestBody ExpenseRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(expenseService.update(tripId, id, req, actorId(principal)));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<ExpenseView> voidExpense(
            @PathVariable String tripId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(expenseService.voidExpense(tripId, id, actorId(principal)));
    }

    @GetMapping("/summary")
    public ResponseEntity<SummaryView> getSummary(
            @PathVariable String tripId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(settlementService.summary(tripId, actorId(principal)));
    }

    @GetMapping("/settlements")
    public ResponseEntity<List<TransferView>> getSettlements(
            @PathVariable String tripId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(settlementService.settlements(tripId, actorId(principal)));
    }

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentView>> getPayments(
            @PathVariable String tripId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(settlementService.payments(tripId, actorId(principal)));
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentView> recordPayment(
            @PathVariable String tripId,
            @RequestBody PaymentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(settlementService.recordPayment(tripId, req, actorId(principal)));
    }

    @PostMapping("/payments/{id}/confirm")
    public ResponseEntity<PaymentView> confirmPayment(
            @PathVariable String tripId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(settlementService.confirm(tripId, id, actorId(principal)));
    }

    @PostMapping("/payments/{id}/reject")
    public ResponseEntity<PaymentView> rejectPayment(
            @PathVariable String tripId,
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(settlementService.reject(tripId, id, actorId(principal)));
    }

    @GetMapping("/budget")
    public ResponseEntity<DashboardView> getBudget(
            @PathVariable String tripId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(budgetService.dashboard(tripId, actorId(principal)));
    }

    @PutMapping("/budget")
    public ResponseEntity<DashboardView> setBudget(
            @PathVariable String tripId,
            @RequestBody BudgetRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(budgetService.setBudget(tripId, req, actorId(principal)));
    }

    @GetMapping("/my-money-prefs")
    public ResponseEntity<PrefView> getMyMoneyPrefs(
            @PathVariable String tripId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(prefService.get(tripId, actorId(principal)));
    }

    @PutMapping("/my-money-prefs")
    public ResponseEntity<PrefView> setMyMoneyPrefs(
            @PathVariable String tripId,
            @RequestBody PrefRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(prefService.set(tripId, req, actorId(principal)));
    }

    private Long actorId(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new UnauthorizedActionException("Authentication required");
        }
        return principal.getId();
    }
}
