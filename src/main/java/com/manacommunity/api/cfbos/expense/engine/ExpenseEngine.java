package com.manacommunity.api.cfbos.expense.engine;

import com.manacommunity.api.cfbos.accounting.dto.JournalEntryRequest;
import com.manacommunity.api.cfbos.accounting.engine.AccountingEngine;
import com.manacommunity.api.cfbos.accounting.entity.JournalEntry;
import com.manacommunity.api.cfbos.approval.dto.ApprovalResponse;
import com.manacommunity.api.cfbos.approval.dto.SubmitApprovalRequest;
import com.manacommunity.api.cfbos.approval.service.ApprovalWorkflowService;
import com.manacommunity.api.cfbos.expense.dto.CreateExpenseRequest;
import com.manacommunity.api.cfbos.expense.entity.CfbosExpense;
import com.manacommunity.api.cfbos.expense.entity.CfbosExpenseLine;
import com.manacommunity.api.cfbos.expense.entity.ExpenseCategory;
import com.manacommunity.api.cfbos.expense.enums.ExpenseStatus;
import com.manacommunity.api.cfbos.expense.repository.CfbosExpenseRepository;
import com.manacommunity.api.cfbos.expense.repository.ExpenseCategoryRepository;
import com.manacommunity.api.cfbos.shared.enums.ApprovalState;
import com.manacommunity.api.cfbos.shared.enums.DocumentType;
import com.manacommunity.api.cfbos.shared.enums.SourceModule;
import com.manacommunity.api.cfbos.shared.exception.CfbosException;
import com.manacommunity.api.cfbos.shared.sequence.service.DocumentSequenceService;
import com.manacommunity.api.cfbos.tax.dto.GstCalculationResult;
import com.manacommunity.api.cfbos.tax.engine.TaxEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseEngine {

    private final CfbosExpenseRepository expenseRepository;
    private final ExpenseCategoryRepository categoryRepository;
    private final DocumentSequenceService documentSequenceService;
    private final TaxEngine taxEngine;
    private final AccountingEngine accountingEngine;
    private final ApprovalWorkflowService approvalWorkflowService;

    private static final BigDecimal DEFAULT_CGST_RATE = new BigDecimal("9.00");
    private static final BigDecimal DEFAULT_SGST_RATE = new BigDecimal("9.00");

    @Transactional
    public CfbosExpense recordExpense(CreateExpenseRequest request) {
        ExpenseCategory category = categoryRepository.findById(request.getExpenseCategoryId())
                .orElseThrow(() -> new CfbosException("Expense category not found: " + request.getExpenseCategoryId()));

        LocalDate expDate = request.getExpenseDate() != null ? request.getExpenseDate() : LocalDate.now();
        String fiscalYear = String.valueOf(expDate.getYear());
        String expenseNumber = documentSequenceService.nextNumber(DocumentType.EXPENSE, fiscalYear);

        CfbosExpense expense = CfbosExpense.builder()
                .expenseNumber(expenseNumber)
                .expenseDate(expDate)
                .expenseCategory(category)
                .description(request.getDescription())
                .paymentMode(request.getPaymentMode() != null ? request.getPaymentMode() : "BANK_TRANSFER")
                .paymentReference(request.getPaymentReference())
                .vendorId(request.getVendorId())
                .costCenterId(request.getCostCenterId())
                .fundId(request.getFundId())
                .status(ExpenseStatus.PENDING_APPROVAL)
                .lines(new ArrayList<>())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;

        if (request.getLines() != null) {
            for (CreateExpenseRequest.ExpenseLineItemRequest lineReq : request.getLines()) {
                BigDecimal qty = lineReq.getQuantity() != null ? lineReq.getQuantity() : BigDecimal.ONE;
                BigDecimal rate = lineReq.getRate() != null ? lineReq.getRate() : BigDecimal.ZERO;
                BigDecimal amount = qty.multiply(rate);
                subtotal = subtotal.add(amount);

                if (Boolean.TRUE.equals(lineReq.getIsTaxable())) {
                    GstCalculationResult gst = taxEngine.calculateGst(amount, DEFAULT_CGST_RATE, DEFAULT_SGST_RATE);
                    totalCgst = totalCgst.add(gst.getCgstAmount());
                    totalSgst = totalSgst.add(gst.getSgstAmount());
                }

                CfbosExpenseLine line = CfbosExpenseLine.builder()
                        .expense(expense)
                        .description(lineReq.getDescription())
                        .accountId(lineReq.getAccountId())
                        .quantity(qty)
                        .rate(rate)
                        .amount(amount)
                        .hsnSacCode(lineReq.getHsnSacCode())
                        .isTaxable(Boolean.TRUE.equals(lineReq.getIsTaxable()))
                        .build();

                expense.getLines().add(line);
            }
        }

        BigDecimal totalTax = totalCgst.add(totalSgst);
        BigDecimal totalAmount = subtotal.add(totalTax);

        expense.setSubtotal(subtotal);
        expense.setCgstAmount(totalCgst);
        expense.setSgstAmount(totalSgst);
        expense.setIgstAmount(BigDecimal.ZERO);
        expense.setTotalTax(totalTax);
        expense.setTotalAmount(totalAmount);

        expense = expenseRepository.save(expense);

        ApprovalResponse approval = approvalWorkflowService.submitRequest(
                SubmitApprovalRequest.builder()
                        .entityType("EXPENSE")
                        .entityId(expense.getId())
                        .amount(totalAmount)
                        .remarks("Expense submission: " + expenseNumber + " - " + request.getDescription())
                        .submittedBy(request.getSubmittedBy() != null ? request.getSubmittedBy() : 1L)
                        .build()
        );

        expense.setApprovalRequestId(approval.getId());

        if (approval.getStatus() == ApprovalState.APPROVED) {
            expense.setStatus(ExpenseStatus.APPROVED);
            postExpenseJournal(expense);
        }

        return expenseRepository.save(expense);
    }

    @Transactional
    public void postExpenseJournal(CfbosExpense expense) {
        try {
            List<JournalEntryRequest.LineRequest> jLines = new ArrayList<>();
            jLines.add(JournalEntryRequest.LineRequest.builder()
                    .accountCode("5000")
                    .debitAmount(expense.getSubtotal())
                    .narration("Expense: " + expense.getExpenseNumber())
                    .costCenterId(expense.getCostCenterId())
                    .fundId(expense.getFundId())
                    .build());

            if (expense.getTotalTax().compareTo(BigDecimal.ZERO) > 0) {
                jLines.add(JournalEntryRequest.LineRequest.builder()
                        .accountCode("1310")
                        .debitAmount(expense.getTotalTax())
                        .narration("Input GST for " + expense.getExpenseNumber())
                        .build());
            }

            jLines.add(JournalEntryRequest.LineRequest.builder()
                    .accountCode("2000")
                    .creditAmount(expense.getTotalAmount())
                    .narration("Payable for " + expense.getExpenseNumber())
                    .build());

            JournalEntry entry = accountingEngine.createAndPostJournalEntry(
                    JournalEntryRequest.builder()
                            .entryDate(expense.getExpenseDate())
                            .sourceModule(SourceModule.EXPENSE)
                            .sourceDocumentType("EXPENSE")
                            .narration("Expense recorded: " + expense.getExpenseNumber())
                            .lines(jLines)
                            .build()
            );
            expense.setJournalEntryId(entry.getId());
            expenseRepository.save(expense);
        } catch (Exception ignored) {}
    }
}
