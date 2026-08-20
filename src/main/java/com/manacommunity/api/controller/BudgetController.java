package com.manacommunity.api.controller;

import com.manacommunity.api.model.BudgetAllocation;
import com.manacommunity.api.model.ExpenseCategory;
import com.manacommunity.api.repository.BudgetAllocationRepository;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/finance/budget")
public class BudgetController {

    private final BudgetAllocationRepository budgetAllocationRepository;
    private final LoggedInUserService loggedInUserService;

    public BudgetController(BudgetAllocationRepository budgetAllocationRepository,
                            LoggedInUserService loggedInUserService) {
        this.budgetAllocationRepository = budgetAllocationRepository;
        this.loggedInUserService = loggedInUserService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<BudgetAllocation>> getBudgets(
            @RequestParam(required = false) String financialYear,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = resolveCommunityId(principal);
        List<BudgetAllocation> allocations = (financialYear != null && !financialYear.isBlank())
                ? budgetAllocationRepository.findByCommunityIdAndFinancialYearOrderByCategoryAsc(communityId, financialYear)
                : budgetAllocationRepository.findByCommunityIdOrderByCategoryAsc(communityId);
        return ResponseEntity.ok(allocations);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<BudgetAllocation> allocateBudget(
            @RequestParam String financialYear,
            @RequestParam ExpenseCategory category,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long communityId = resolveCommunityId(principal);
        Optional<BudgetAllocation> existing = budgetAllocationRepository
                .findByCommunityIdAndFinancialYearAndCategory(communityId, financialYear, category);

        BudgetAllocation allocation;
        if (existing.isPresent()) {
            allocation = existing.get();
            allocation.setAllocatedAmount(amount);
            if (notes != null) {
                allocation.setNotes(notes);
            }
        } else {
            allocation = BudgetAllocation.builder()
                    .communityId(communityId)
                    .financialYear(financialYear)
                    .category(category)
                    .allocatedAmount(amount)
                    .spentAmount(BigDecimal.ZERO)
                    .notes(notes)
                    .build();
        }

        return ResponseEntity.ok(budgetAllocationRepository.save(allocation));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Void> deleteAllocation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        budgetAllocationRepository.deleteByCommunityIdAndId(communityId, id);
        return ResponseEntity.ok().build();
    }

    private Long resolveCommunityId(UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return user.getCommunity() != null ? user.getCommunity().getId() : null;
    }
}
