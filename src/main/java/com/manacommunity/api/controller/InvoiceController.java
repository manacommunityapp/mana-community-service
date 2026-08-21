package com.manacommunity.api.controller;

import com.manacommunity.api.dto.InvoiceDto;
import com.manacommunity.api.dto.PaymentRequest;
import com.manacommunity.api.model.VendorInvoice;
import com.manacommunity.api.service.InvoiceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/inventory/expenses", "/api/asset-finance/invoices"})
@PreAuthorize("hasAuthority('View Admin')")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final LoggedInUserService loggedInUserService;

    public InvoiceController(InvoiceService invoiceService, LoggedInUserService loggedInUserService) {
        this.invoiceService = invoiceService;
        this.loggedInUserService = loggedInUserService;
    }

    @GetMapping
    public ResponseEntity<List<VendorInvoice>> getInvoices(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(invoiceService.getAllInvoices(communityId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VendorInvoice> getInvoiceById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return invoiceService.getInvoiceById(id, communityId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload")
    public ResponseEntity<VendorInvoice> uploadInvoice(
            @RequestBody InvoiceDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return ResponseEntity.ok(invoiceService.createInvoiceFromOcr(dto, communityId));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<VendorInvoice> approveInvoice(
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        String approvalNotes = notes != null ? notes : "Approved by Treasurer";
        return invoiceService.approveInvoice(id, communityId, approvalNotes)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<VendorInvoice> rejectInvoice(
            @PathVariable Long id,
            @RequestParam(required = false) String notes,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        String rejectionNotes = notes != null ? notes : "Rejected by Treasurer";
        return invoiceService.rejectInvoice(id, communityId, rejectionNotes)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<VendorInvoice> markPaid(
            @PathVariable Long id,
            @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = resolveCommunityId(principal);
        return invoiceService.markPaid(id, communityId, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private Long resolveCommunityId(UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return user.getCommunity() != null ? user.getCommunity().getId() : null;
    }
}
