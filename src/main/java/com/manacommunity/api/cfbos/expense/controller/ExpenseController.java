package com.manacommunity.api.cfbos.expense.controller;

import com.manacommunity.api.cfbos.expense.dto.CreateExpenseRequest;
import com.manacommunity.api.cfbos.expense.dto.ExpenseResponse;
import com.manacommunity.api.cfbos.expense.entity.ExpenseCategory;
import com.manacommunity.api.cfbos.expense.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("cfbosExpenseController")
@RequestMapping("/api/cfbos/expenses")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('Manage Finance') or hasAuthority('Admin') or hasAuthority('Finance Admin')")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ExpenseResponse> createExpense(@RequestBody CreateExpenseRequest request) {
        return ResponseEntity.ok(expenseService.createExpense(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(expenseService.getExpenseById(id));
    }

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getAll(
            @RequestParam(required = false) Long communityId) {
        if (communityId != null) {
            return ResponseEntity.ok(expenseService.getExpensesByCommunity(communityId));
        }
        return ResponseEntity.ok(expenseService.getAllExpenses());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<ExpenseCategory>> getCategories() {
        return ResponseEntity.ok(expenseService.getCategories());
    }

    @PostMapping("/categories")
    public ResponseEntity<ExpenseCategory> createCategory(@RequestBody ExpenseCategory category) {
        return ResponseEntity.ok(expenseService.createCategory(category));
    }
}
