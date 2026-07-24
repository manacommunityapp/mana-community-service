package com.manacommunity.api.vendor.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.dto.VendorServiceRequest;
import com.manacommunity.api.vendor.dto.VendorServiceResponse;
import com.manacommunity.api.vendor.service.VmsServiceCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor/services")
@RequiredArgsConstructor
public class VmsServiceCatalogController {

    private final VmsServiceCatalogService serviceCatalogService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<Page<VendorServiceResponse>> getServices(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(serviceCatalogService.searchServices(user.getCommunity().getId(), search, pageable));
        }
        return ResponseEntity.ok(serviceCatalogService.getServices(user.getCommunity().getId(), categoryId, pageable));
    }

    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<List<VendorServiceResponse>> getVendorServices(@PathVariable Long vendorId) {
        return ResponseEntity.ok(serviceCatalogService.getVendorServices(vendorId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<VendorServiceResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceCatalogService.getById(id));
    }

    @PostMapping("/vendor/{vendorId}")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<VendorServiceResponse> create(
            @PathVariable Long vendorId,
            @Valid @RequestBody VendorServiceRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceCatalogService.create(req, vendorId, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<VendorServiceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody VendorServiceRequest req) {
        return ResponseEntity.ok(serviceCatalogService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        serviceCatalogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
