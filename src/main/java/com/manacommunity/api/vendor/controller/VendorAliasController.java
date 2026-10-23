package com.manacommunity.api.vendor.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.dto.RatingResponse;
import com.manacommunity.api.vendor.dto.VendorResponse;
import com.manacommunity.api.vendor.entity.VmsInvoice;
import com.manacommunity.api.vendor.repository.VmsInvoiceRepository;
import com.manacommunity.api.vendor.service.VmsRatingService;
import com.manacommunity.api.vendor.service.VmsVendorManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/vendor")
@RequiredArgsConstructor
public class VendorAliasController {

    private final VmsRatingService ratingService;
    private final VmsVendorManagementService vendorService;
    private final VmsInvoiceRepository invoiceRepository;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/reviews")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<Page<RatingResponse>> getReviews(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        if (vendorId != null) {
            return ResponseEntity.ok(ratingService.getVendorRatings(vendorId, pageable));
        }
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ratingService.getVendorRatings(user.getId(), pageable));
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<Page<VmsInvoice>> getInvoices(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        if (vendorId != null) {
            return ResponseEntity.ok(invoiceRepository.findByVendorId(vendorId, pageable));
        }
        if (status != null && !status.isBlank()) {
            try {
                VmsInvoice.InvoiceStatus invoiceStatus = VmsInvoice.InvoiceStatus.valueOf(status.toUpperCase());
                return ResponseEntity.ok(invoiceRepository.findByCommunityIdAndStatus(communityId, invoiceStatus, pageable));
            } catch (IllegalArgumentException ignored) {}
        }
        return ResponseEntity.ok(invoiceRepository.findByCommunityId(communityId, pageable));
    }

    @PostMapping("/invoices/{id}/send")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<?> sendInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        Optional<VmsInvoice> invoiceOpt = invoiceRepository.findByIdAndCommunityId(id, communityId);
        if (invoiceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        VmsInvoice invoice = invoiceOpt.get();
        if (invoice.getStatus() == VmsInvoice.InvoiceStatus.DRAFT) {
            invoice.setStatus(VmsInvoice.InvoiceStatus.SENT);
            invoiceRepository.save(invoice);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", invoice.getId());
        result.put("invoiceNumber", invoice.getInvoiceNumber());
        result.put("status", invoice.getStatus().name());
        result.put("message", "Invoice sent successfully");
        return ResponseEntity.ok(result);
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<VendorResponse> getVendorProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vendorService.getMyVendorProfile(user.getId(), user.getCommunity().getId()));
    }
}
