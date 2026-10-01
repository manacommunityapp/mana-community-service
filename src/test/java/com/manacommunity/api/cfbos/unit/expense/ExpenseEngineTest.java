package com.manacommunity.api.cfbos.unit.expense;
import com.manacommunity.api.sports.model.*;
import com.manacommunity.api.sports.repository.*;
import com.manacommunity.api.sports.dto.*;
import com.manacommunity.api.sports.service.*;
import com.manacommunity.api.sports.scheduler.*;
import com.manacommunity.api.sports.controller.*;

import com.manacommunity.api.cfbos.accounting.engine.AccountingEngine;
import com.manacommunity.api.cfbos.approval.dto.ApprovalResponse;
import com.manacommunity.api.cfbos.approval.service.ApprovalWorkflowService;
import com.manacommunity.api.cfbos.expense.dto.CreateExpenseRequest;
import com.manacommunity.api.cfbos.expense.engine.ExpenseEngine;
import com.manacommunity.api.cfbos.expense.entity.CfbosExpense;
import com.manacommunity.api.cfbos.expense.entity.ExpenseCategory;
import com.manacommunity.api.cfbos.expense.enums.ExpenseStatus;
import com.manacommunity.api.cfbos.expense.repository.CfbosExpenseRepository;
import com.manacommunity.api.cfbos.expense.repository.ExpenseCategoryRepository;
import com.manacommunity.api.cfbos.shared.enums.ApprovalState;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import com.manacommunity.api.cfbos.tax.dto.GstCalculationResult;
import com.manacommunity.api.cfbos.tax.engine.TaxEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseEngineTest {

    @Mock private CfbosExpenseRepository expenseRepository;
    @Mock private ExpenseCategoryRepository categoryRepository;
    @Mock private DocumentSequenceService documentSequenceService;
    @Mock private TaxEngine taxEngine;
    @Mock private AccountingEngine accountingEngine;
    @Mock private ApprovalWorkflowService approvalWorkflowService;

    private ExpenseEngine expenseEngine;

    @BeforeEach
    void setUp() {
        expenseEngine = new ExpenseEngine(
                expenseRepository, categoryRepository, documentSequenceService, taxEngine, accountingEngine, approvalWorkflowService
        );
    }

    @Test
    @DisplayName("Record expense computes total tax and routes for approval")
    void recordExpenseWithTaxAndApproval() {
        ExpenseCategory cat = ExpenseCategory.builder().id(1L).code("FACILITY").name("Facility Repairs").build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(documentSequenceService.nextNumber(any(), anyString())).thenReturn("EXP-2026-0001");
        when(taxEngine.calculateGst(any(), any(), any()))
                .thenReturn(GstCalculationResult.builder()
                        .taxableAmount(new BigDecimal("10000.00"))
                        .cgstRate(new BigDecimal("9.00"))
                        .cgstAmount(new BigDecimal("900.00"))
                        .sgstRate(new BigDecimal("9.00"))
                        .sgstAmount(new BigDecimal("900.00"))
                        .totalTax(new BigDecimal("1800.00"))
                        .build());

        when(expenseRepository.save(any(CfbosExpense.class))).thenAnswer(i -> {
            CfbosExpense e = i.getArgument(0);
            e.setId(50L);
            return e;
        });

        when(approvalWorkflowService.submitRequest(any())).thenReturn(
                ApprovalResponse.builder().id(1L).status(ApprovalState.APPROVED).build()
        );

        CreateExpenseRequest req = CreateExpenseRequest.builder()
                .expenseCategoryId(1L)
                .expenseDate(LocalDate.of(2026, 4, 10))
                .description("Elevator repair parts")
                .lines(List.of(
                        CreateExpenseRequest.ExpenseLineItemRequest.builder()
                                .description("Motor cable")
                                .quantity(new BigDecimal("2"))
                                .rate(new BigDecimal("5000.00"))
                                .hsnSacCode("8431")
                                .isTaxable(true)
                                .build()
                ))
                .build();

        CfbosExpense expense = expenseEngine.recordExpense(req);

        assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.APPROVED);
        assertThat(expense.getSubtotal()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(expense.getTotalTax()).isEqualByComparingTo(new BigDecimal("1800.00"));
        assertThat(expense.getTotalAmount()).isEqualByComparingTo(new BigDecimal("11800.00"));
    }
}
