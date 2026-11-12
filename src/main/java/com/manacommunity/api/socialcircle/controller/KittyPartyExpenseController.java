package com.manacommunity.api.socialcircle.controller;

import com.manacommunity.api.socialcircle.dto.KittyPartyExpenseDtos.*;
import com.manacommunity.api.socialcircle.service.KittyPartyExpenseSplitService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kitty-expenses")
@RequiredArgsConstructor
public class KittyPartyExpenseController {

    private final KittyPartyExpenseSplitService expenseSplitService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/split")
    public ResponseEntity<List<ExpenseShareResponse>> splitExpense(
            @Valid @RequestBody SplitExpenseRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(expenseSplitService.splitExpense(request.getExpenseId(), request, user, idempotencyKey));
    }

    @GetMapping("/expenses/{expenseId}/shares")
    public ResponseEntity<List<ExpenseShareResponse>> getSharesForExpense(
            @PathVariable Long expenseId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        loggedInUserService.resolve(principal);
        return ResponseEntity.ok(expenseSplitService.getSharesForExpense(expenseId));
    }

    @GetMapping("/my-pending")
    public ResponseEntity<List<ExpenseShareResponse>> getMyPendingShares(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(expenseSplitService.getMyPendingShares(user.getId()));
    }

    @PostMapping("/shares/{shareId}/settle")
    public ResponseEntity<ExpenseShareResponse> settleShare(
            @PathVariable Long shareId,
            @Valid @RequestBody SettleShareRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(expenseSplitService.settleShare(shareId, user, request, idempotencyKey));
    }
}
