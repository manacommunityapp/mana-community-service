package com.manacommunity.api.vendor.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.dto.RatingResponse;
import com.manacommunity.api.vendor.dto.InvoiceRequest;
import com.manacommunity.api.vendor.dto.InvoiceResponse;
import jakarta.validation.Valid;
import com.manacommunity.api.vendor.dto.VendorResponse;
import com.manacommunity.api.vendor.dto.VendorRequest;
import com.manacommunity.api.vendor.entity.VmsInvoice;
import com.manacommunity.api.vendor.repository.VmsInvoiceRepository;
import com.manacommunity.api.vendor.service.VmsRatingService;
import com.manacommunity.api.vendor.service.VmsVendorManagementService;
import com.manacommunity.api.vendor.service.VmsInvoiceService;
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
    private final VmsInvoiceService invoiceService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/reviews")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<Page<RatingResponse>> getReviews(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        AppUser user = loggedInUserService.resolve(principal);
        Long resolvedVendorId = vendorId != null ? vendorId
                : vendorService.getMyVendorProfile(user.getId(), user.getCommunity().getId()).getId();
        return ResponseEntity.ok(ratingService.getVendorRatings(resolvedVendorId, user.getCommunity().getId(), pageable));
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<Page<InvoiceResponse>> getInvoices(
            @RequestParam(required = false) Long vendorId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(invoiceService.listCommunity(communityId, vendorId, status, pageable));
    }

    @GetMapping("/invoices/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<InvoiceResponse>> getMyInvoices(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(invoiceService.listMine(user, PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(invoiceService.get(id, user.getCommunity().getId()));
    }

    @PostMapping("/invoices")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody InvoiceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        boolean manager = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("Manage Vendors"));
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(invoiceService.create(request, user, user.getCommunity(), manager));
    }

    @PutMapping("/invoices/{id}/status")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<InvoiceResponse> updateInvoiceStatus(@PathVariable Long id, @RequestParam String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(invoiceService.updateStatus(id, status, user.getCommunity().getId()));
    }

    @PostMapping("/invoices/{id}/send")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<?> sendInvoice(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        invoiceService.send(id, communityId);
        return ResponseEntity.ok(invoiceService.get(id, communityId));
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VendorResponse> updateMyVendorProfile(@Valid @RequestBody VendorRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vendorService.updateMyVendorProfile(request, user));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<VendorResponse> getVendorProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(vendorService.getMyVendorProfile(user.getId(), user.getCommunity().getId()));
    }
}
