package com.manacommunity.api.controller;

import com.manacommunity.api.model.BudgetAllocation;
import com.manacommunity.api.model.ExpenseCategory;
import com.manacommunity.api.repository.BudgetAllocationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/finance/budget")
public class BudgetController {

    private final BudgetAllocationRepository budgetAllocationRepository;

    public BudgetController(BudgetAllocationRepository budgetAllocationRepository) {
        this.budgetAllocationRepository = budgetAllocationRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<BudgetAllocation>> getBudgets(
            @RequestParam(required = false) String financialYear) {
        
        List<BudgetAllocation> allocations;
        if (financialYear != null && !financialYear.isBlank()) {
            allocations = budgetAllocationRepository.findByFinancialYear(financialYear);
        } else {
            allocations = budgetAllocationRepository.findAll();
        }
        return ResponseEntity.ok(allocations);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<BudgetAllocation> allocateBudget(
            @RequestParam String financialYear,
            @RequestParam ExpenseCategory category,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String notes) {

        Optional<BudgetAllocation> existing = budgetAllocationRepository
                .findByFinancialYearAndCategory(financialYear, category);

        BudgetAllocation allocation;
        if (existing.isPresent()) {
            allocation = existing.get();
            allocation.setAllocatedAmount(amount);
            if (notes != null) {
                allocation.setNotes(notes);
            }
        } else {
            allocation = BudgetAllocation.builder()
                    .financialYear(financialYear)
                    .category(category)
                    .allocatedAmount(amount)
                    .spentAmount(BigDecimal.ZERO)
                    .notes(notes)
                    .build();
        }

        BudgetAllocation saved = budgetAllocationRepository.save(allocation);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Void> deleteAllocation(@PathVariable Long id) {
        budgetAllocationRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
