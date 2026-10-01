package com.manacommunity.api.cfbos.expense.service;

import com.manacommunity.api.cfbos.expense.dto.CreateExpenseRequest;
import com.manacommunity.api.cfbos.expense.dto.ExpenseResponse;
import com.manacommunity.api.cfbos.expense.engine.ExpenseEngine;
import com.manacommunity.api.cfbos.expense.entity.CfbosExpense;
import com.manacommunity.api.cfbos.expense.entity.ExpenseCategory;
import com.manacommunity.api.cfbos.expense.repository.CfbosExpenseRepository;
import com.manacommunity.api.cfbos.expense.repository.ExpenseCategoryRepository;
import com.manacommunity.api.cfbos.shared.exception.CfbosResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseEngine expenseEngine;
    private final CfbosExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;

    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request) {
        CfbosExpense expense = expenseEngine.recordExpense(request);
        return toResponse(expense);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(Long id) {
        CfbosExpense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new CfbosResourceNotFoundException("Expense", id));
        return toResponse(expense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAllExpenses() {
        return expenseRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExpenseCategory> getCategories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public ExpenseCategory createCategory(ExpenseCategory category) {
        return categoryRepository.save(category);
    }

    private ExpenseResponse toResponse(CfbosExpense e) {
        return ExpenseResponse.builder()
                .id(e.getId())
                .expenseNumber(e.getExpenseNumber())
                .expenseDate(e.getExpenseDate())
                .expenseCategoryId(e.getExpenseCategory() != null ? e.getExpenseCategory().getId() : null)
                .expenseCategoryName(e.getExpenseCategory() != null ? e.getExpenseCategory().getName() : "")
                .description(e.getDescription())
                .subtotal(e.getSubtotal())
                .cgstAmount(e.getCgstAmount())
                .sgstAmount(e.getSgstAmount())
                .igstAmount(e.getIgstAmount())
                .totalTax(e.getTotalTax())
                .totalAmount(e.getTotalAmount())
                .paymentMode(e.getPaymentMode())
                .paymentReference(e.getPaymentReference())
                .vendorId(e.getVendorId())
                .status(e.getStatus())
                .approvalRequestId(e.getApprovalRequestId())
                .journalEntryId(e.getJournalEntryId())
                .lines(e.getLines() != null ? e.getLines().stream().map(l ->
                        ExpenseResponse.ExpenseLineDto.builder()
                                .id(l.getId())
                                .description(l.getDescription())
                                .accountId(l.getAccountId())
                                .quantity(l.getQuantity())
                                .rate(l.getRate())
                                .amount(l.getAmount())
                                .hsnSacCode(l.getHsnSacCode())
                                .isTaxable(l.getIsTaxable())
                                .build()
                ).collect(Collectors.toList()) : List.of())
                .build();
    }
}
