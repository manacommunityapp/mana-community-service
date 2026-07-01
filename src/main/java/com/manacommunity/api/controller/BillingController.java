package com.manacommunity.api.controller;

import com.manacommunity.api.dto.PagedResponse;
import com.manacommunity.api.model.AppUser;
import com.manacommunity.api.model.Expense;
import com.manacommunity.api.model.Invoice;
import com.manacommunity.api.repository.ExpenseRepository;
import com.manacommunity.api.service.LoggedInUserService;
import com.manacommunity.api.repository.InvoiceRepository;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.BillingService;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private static final int MAX_PAGE_SIZE = 50;

    private final ExpenseRepository expenseRepository;
    private final InvoiceRepository invoiceRepository;
    private final BillingService billingService;
    private final LoggedInUserService loggedInUserService;

    public BillingController(ExpenseRepository expenseRepository,
                             InvoiceRepository invoiceRepository,
                             BillingService billingService,
                             LoggedInUserService loggedInUserService) {
        this.expenseRepository = expenseRepository;
        this.invoiceRepository = invoiceRepository;
        this.billingService = billingService;
        this.loggedInUserService = loggedInUserService;
    }

    // ── Expenses ──────────────────────────────────────────────────────────

    @GetMapping("/expenses")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<PagedResponse<ExpenseResponse>> getExpenses(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        size = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var pageable = PageRequest.of(page, size);
        AppUser user = loggedInUserService.resolve(principal);
        var communityId = user.getCommunity().getId();

        var expensePage = status != null && !status.isBlank()
                ? expenseRepository.findByCommunityIdAndStatusOrderByCreatedAtDesc(communityId, status, pageable)
                : expenseRepository.findByCommunityIdOrderByCreatedAtDesc(communityId, pageable);

        return ResponseEntity.ok(PagedResponse.from(expensePage, this::toExpenseResponse));
    }

    @PostMapping("/expenses")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ExpenseResponse> createExpense(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String title,
            @RequestParam BigDecimal amount,
            @RequestParam String category,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile receipt) {

        AppUser user = loggedInUserService.resolve(principal);
        Expense expense = Expense.builder()
                .title(title)
                .amount(amount)
                .category(category)
                .description(description)
                .community(user.getCommunity())
                .createdBy(user)
                .build();

        if (receipt != null && !receipt.isEmpty()) {
            expense.setReceiptFileName(receipt.getOriginalFilename());
            // Mock — in production, upload to S3 and store the URL
            expense.setReceiptUrl("https://mana-community-receipts.s3.ap-south-1.amazonaws.com/receipts/" + receipt.getOriginalFilename());
        }

        expense = expenseRepository.save(expense);
        return ResponseEntity.ok(toExpenseResponse(expense));
    }

    @PostMapping("/expenses/{id}/approve")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Object>> approveExpense(@PathVariable Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        expense.setStatus("APPROVED");
        expenseRepository.save(expense);

        billingService.batchGenerateInvoices(id);

        return ResponseEntity.ok(Map.of(
                "message", "Expense approved. Invoice generation started asynchronously.",
                "expenseId", id
        ));
    }

    @PostMapping("/expenses/{id}/reject")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, String>> rejectExpense(@PathVariable Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));
        expense.setStatus("REJECTED");
        expenseRepository.save(expense);
        return ResponseEntity.ok(Map.of("message", "Expense rejected"));
    }

    // ── Invoices ──────────────────────────────────────────────────────────

    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<PagedResponse<InvoiceResponse>> getInvoices(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        size = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        var pageable = PageRequest.of(page, size);
        AppUser user = loggedInUserService.resolve(principal);
        var communityId = user.getCommunity().getId();

        var invoicePage = status != null && !status.isBlank()
                ? invoiceRepository.findByCommunityIdAndStatusOrderByGeneratedAtDesc(communityId, status, pageable)
                : invoiceRepository.findByCommunityIdOrderByGeneratedAtDesc(communityId, pageable);

        return ResponseEntity.ok(PagedResponse.from(invoicePage, this::toInvoiceResponse));
    }

    @GetMapping("/invoices/my")
    public ResponseEntity<List<InvoiceResponse>> getMyInvoices(
            @AuthenticationPrincipal UserPrincipal principal) {
        var invoices = invoiceRepository.findByResidentIdOrderByGeneratedAtDesc(principal.getId());
        return ResponseEntity.ok(invoices.stream().map(this::toInvoiceResponse).toList());
    }

    @PostMapping("/invoices/{id}/pay")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<InvoiceResponse> markAsPaid(@PathVariable Long id) {
        Invoice invoice = billingService.markAsPaid(id);
        return ResponseEntity.ok(toInvoiceResponse(invoice));
    }

    @GetMapping("/gst-preview")
    public ResponseEntity<BillingService.GstBreakdown> previewGst(@RequestParam BigDecimal amount) {
        return ResponseEntity.ok(billingService.calculateGst(amount));
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private ExpenseResponse toExpenseResponse(Expense e) {
        return ExpenseResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .amount(e.getAmount())
                .category(e.getCategory())
                .description(e.getDescription())
                .receiptFileName(e.getReceiptFileName())
                .receiptUrl(e.getReceiptUrl())
                .status(e.getStatus())
                .createdBy(e.getCreatedBy() != null ? e.getCreatedBy().getFullName() : null)
                .createdAt(e.getCreatedAt())
                .build();
    }

    private InvoiceResponse toInvoiceResponse(Invoice i) {
        return InvoiceResponse.builder()
                .id(i.getId())
                .invoiceNumber(i.getInvoiceNumber())
                .residentName(i.getResident() != null ? i.getResident().getFullName() : null)
                .residentId(i.getResident() != null ? i.getResident().getId() : null)
                .flatNo(i.getResident() != null ? i.getResident().getFlatNo() : null)
                .taxableAmount(i.getTaxableAmount())
                .cgst(i.getCgst())
                .sgst(i.getSgst())
                .totalAmount(i.getTotalAmount())
                .dueDate(i.getDueDate().toString())
                .status(i.getStatus())
                .pdfUrl(i.getPdfUrl())
                .eventTitle(i.getExpense() != null ? i.getExpense().getTitle() : null)
                .generatedAt(i.getGeneratedAt())
                .paidAt(i.getPaidAt())
                .build();
    }

    @Data
    @Builder
    public static class ExpenseResponse {
        private Long id;
        private String title;
        private BigDecimal amount;
        private String category;
        private String description;
        private String receiptFileName;
        private String receiptUrl;
        private String status;
        private String createdBy;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class InvoiceResponse {
        private Long id;
        private String invoiceNumber;
        private String residentName;
        private Long residentId;
        private String flatNo;
        private BigDecimal taxableAmount;
        private BigDecimal cgst;
        private BigDecimal sgst;
        private BigDecimal totalAmount;
        private String dueDate;
        private String status;
        private String pdfUrl;
        private String eventTitle;
        private LocalDateTime generatedAt;
        private LocalDateTime paidAt;
    }
}
