package com.manacommunity.api.vendor.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.dto.VendorCategoryRequest;
import com.manacommunity.api.vendor.dto.VendorCategoryResponse;
import com.manacommunity.api.vendor.service.VmsVendorCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor/categories")
@RequiredArgsConstructor
public class VmsVendorCategoryController {

    private final VmsVendorCategoryService categoryService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<List<VendorCategoryResponse>> getCategories(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(categoryService.getCategories(user.getCommunity().getId()));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<List<VendorCategoryResponse>> getAllCategories(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(categoryService.getAllCategories(user.getCommunity().getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<VendorCategoryResponse> create(
            @Valid @RequestBody VendorCategoryRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.create(req, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<VendorCategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody VendorCategoryRequest req) {
        return ResponseEntity.ok(categoryService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
