package com.manacommunity.api.controller;

import com.manacommunity.api.dto.PagedResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.Invoice;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.repository.InvoiceRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.BillingService;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Billing endpoints: resident invoices (generated from approved expenses) and the
 * GST preview. Expense CRUD/approval lives in its own {@code /api/expenses} module
 * ({@link ExpenseController} / {@code ExpenseService}); approving an expense there
 * triggers {@link BillingService#batchGenerateInvoices} to fan out these invoices.
 */
@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private static final int MAX_PAGE_SIZE = 50;

    private final InvoiceRepository invoiceRepository;
    private final BillingService billingService;
    private final LoggedInUserService loggedInUserService;

    public BillingController(InvoiceRepository invoiceRepository,
                             BillingService billingService,
                             LoggedInUserService loggedInUserService) {
        this.invoiceRepository = invoiceRepository;
        this.billingService = billingService;
        this.loggedInUserService = loggedInUserService;
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
