package com.manacommunity.api.controller;

import com.manacommunity.api.dto.ExpenseRequest;
import com.manacommunity.api.dto.ExpenseResponse;
import com.manacommunity.api.dto.ExpenseSummaryResponse;
import com.manacommunity.api.dto.PagedResponse;
import com.manacommunity.api.service.ExpenseService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

/**
 * Dedicated REST API for community expenses (the "Expenses" admin pages).
 *
 * <p>All endpoints are community-scoped via the authenticated user and gated by
 * the {@code View Admin} permission, matching the existing billing endpoints.
 * Reads/writes are delegated to {@link ExpenseService}.</p>
 */
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final LoggedInUserService loggedInUserService;

    public ExpenseController(ExpenseService expenseService, LoggedInUserService loggedInUserService) {
        this.expenseService = expenseService;
        this.loggedInUserService = loggedInUserService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<PagedResponse<ExpenseResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        Long communityId = communityId(principal);
        return ResponseEntity.ok(expenseService.list(communityId, status, page, size));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ExpenseSummaryResponse> summary(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(expenseService.summary(communityId(principal)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ExpenseResponse> get(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(expenseService.get(id, communityId(principal)));
    }

    /**
     * Create an expense. Accepts multipart form data (so an optional receipt file
     * can be attached), matching the existing frontend upload flow.
     */
    @PostMapping(consumes = {"multipart/form-data"})
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ExpenseResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String title,
            @RequestParam BigDecimal amount,
            @RequestParam String category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile receipt) {

        AppUser user = requireMember(principal);
        ExpenseRequest req = new ExpenseRequest(title, amount, category, description);
        return ResponseEntity.ok(expenseService.create(req, receipt, user));
    }

    /** Create an expense with a JSON body (no receipt). */
    @PostMapping(consumes = {"application/json"})
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ExpenseResponse> createJson(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ExpenseRequest req) {

        AppUser user = requireMember(principal);
        return ResponseEntity.ok(expenseService.create(req, null, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ExpenseResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest req) {
        return ResponseEntity.ok(expenseService.update(id, req, null, communityId(principal)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ExpenseResponse> approve(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(expenseService.approve(id, communityId(principal)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ExpenseResponse> reject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(expenseService.reject(id, communityId(principal)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        expenseService.delete(id, communityId(principal));
        return ResponseEntity.noContent().build();
    }

    private Long communityId(UserPrincipal principal) {
        return requireMember(principal).getCommunity().getId();
    }

    /** Resolves the caller and asserts they belong to a community (expenses are community-scoped). */
    private AppUser requireMember(UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        if (user.getCommunity() == null) {
            throw new IllegalArgumentException(
                    "Expenses are scoped to a community — this account is not a member of one.");
        }
        return user;
    }
}
