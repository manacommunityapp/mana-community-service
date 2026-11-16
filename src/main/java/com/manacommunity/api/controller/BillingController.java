package com.manacommunity.api.controller;

import com.manacommunity.api.dto.PagedResponse;
import com.manacommunity.api.dto.ResidentInvoiceResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.model.Community;
import com.manacommunity.api.model.Invoice;
import com.manacommunity.api.user.repository.AppUserRepository;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.repository.CommunityRepository;
import com.manacommunity.api.repository.InvoiceRepository;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.BillingService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

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
    private final AppUserRepository appUserRepository;
    private final CommunityRepository communityRepository;

    public BillingController(InvoiceRepository invoiceRepository,
                             BillingService billingService,
                             LoggedInUserService loggedInUserService,
                             AppUserRepository appUserRepository,
                             CommunityRepository communityRepository) {
        this.invoiceRepository = invoiceRepository;
        this.billingService = billingService;
        this.loggedInUserService = loggedInUserService;
        this.appUserRepository = appUserRepository;
        this.communityRepository = communityRepository;
    }

    // ── Invoices ──────────────────────────────────────────────────────────

    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<PagedResponse<ResidentInvoiceResponse>> getInvoices(
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
    public ResponseEntity<List<ResidentInvoiceResponse>> getMyInvoices(
            @AuthenticationPrincipal UserPrincipal principal) {
        var invoices = invoiceRepository.findByResidentIdOrderByGeneratedAtDesc(principal.getId());
        return ResponseEntity.ok(invoices.stream().map(this::toInvoiceResponse).toList());
    }

    @PostMapping("/invoices/{id}/pay")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<ResidentInvoiceResponse> markAsPaid(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        Invoice invoice = billingService.markAsPaid(id, communityId);
        return ResponseEntity.ok(toInvoiceResponse(invoice));
    }

    @GetMapping("/gst-preview")
    public ResponseEntity<BillingService.GstBreakdown> previewGst(@RequestParam BigDecimal amount) {
        return ResponseEntity.ok(billingService.calculateGst(amount));
    }

    @PostMapping("/generate-bulk")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Map<String, Object>> generateBulk(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        AppUser adminUser = loggedInUserService.resolve(principal);
        Long communityId = adminUser.getCommunity().getId();
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new IllegalArgumentException("Community not found"));

        List<AppUser> residents = appUserRepository.findByCommunityIdAndIsActiveTrue(communityId);
        if (residents.isEmpty()) {
            return ResponseEntity.ok(Map.of("generated", 0, "message", "No active residents found"));
        }

        BigDecimal maintenancePerSqft = body.get("maintenancePerSqft") != null
                ? new BigDecimal(body.get("maintenancePerSqft").toString()) : BigDecimal.ZERO;
        BigDecimal sinkingFund = body.get("sinkingFund") != null
                ? new BigDecimal(body.get("sinkingFund").toString()) : BigDecimal.ZERO;
        BigDecimal waterCharges = body.get("waterChargesPerUnit") != null
                ? new BigDecimal(body.get("waterChargesPerUnit").toString()) : BigDecimal.ZERO;
        BigDecimal dgCharges = body.get("dgCharges") != null
                ? new BigDecimal(body.get("dgCharges").toString()) : BigDecimal.ZERO;
        boolean applyGst = body.get("applyGst") == null || Boolean.parseBoolean(body.get("applyGst").toString());
        int dueInDays = body.get("dueInDays") != null ? Integer.parseInt(body.get("dueInDays").toString()) : 15;
        String billingMonth = body.get("billingMonth") != null ? body.get("billingMonth").toString() : "";

        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        LocalDate dueDate = LocalDate.now().plusDays(dueInDays);
        long baseSeq = invoiceRepository.countByCommunityId(communityId);

        int generated = 0;
        for (AppUser resident : residents) {
            BigDecimal taxable = sinkingFund.add(waterCharges).add(dgCharges);
            BigDecimal cgst = BigDecimal.ZERO;
            BigDecimal sgst = BigDecimal.ZERO;
            if (applyGst) {
                BillingService.GstBreakdown gst = billingService.calculateGst(taxable);
                cgst = gst.cgst();
                sgst = gst.sgst();
            }
            BigDecimal total = taxable.add(cgst).add(sgst);
            String invoiceNumber = String.format("INV-%s-%04d", datePrefix, baseSeq + generated + 1);
            String notes = "{\"billingMonth\":\"" + billingMonth + "\",\"maintenancePerSqft\":"
                    + maintenancePerSqft + ",\"sinkingFund\":" + sinkingFund
                    + ",\"waterCharges\":" + waterCharges + ",\"dgCharges\":" + dgCharges + "}";

            Invoice invoice = Invoice.builder()
                    .invoiceNumber(invoiceNumber)
                    .resident(resident)
                    .community(community)
                    .taxableAmount(taxable)
                    .cgst(cgst)
                    .sgst(sgst)
                    .totalAmount(total)
                    .dueDate(dueDate)
                    .status("UNPAID")
                    .build();
            invoiceRepository.save(invoice);
            generated++;
        }

        return ResponseEntity.ok(Map.of("generated", generated, "billingMonth", billingMonth));
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private ResidentInvoiceResponse toInvoiceResponse(Invoice i) {
        return ResidentInvoiceResponse.builder()
                .id(i.getId())
                .invoiceNumber(i.getInvoiceNumber())
                .residentName(i.getResident() != null ? i.getResident().getFullName() : null)
                .residentId(i.getResident() != null ? i.getResident().getId() : null)
                .flatNo(i.getResident() != null ? i.getResident().getFlatNo() : null)
                .taxableAmount(i.getTaxableAmount())
                .cgst(i.getCgst())
                .sgst(i.getSgst())
                .totalAmount(i.getTotalAmount())
                .dueDate(i.getDueDate() != null ? i.getDueDate().toString() : null)
                .status(i.getStatus())
                .pdfUrl(i.getPdfUrl())
                .eventTitle(i.getExpense() != null ? i.getExpense().getTitle() : null)
                .generatedAt(i.getGeneratedAt())
                .paidAt(i.getPaidAt())
                .build();
    }
}
