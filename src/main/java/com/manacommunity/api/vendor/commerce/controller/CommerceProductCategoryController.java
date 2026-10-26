package com.manacommunity.api.vendor.commerce.controller;

import com.manacommunity.api.constants.PermissionConstants;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.commerce.dto.CommerceProductCategoryRequest;
import com.manacommunity.api.vendor.commerce.dto.CommerceProductCategoryResponse;
import com.manacommunity.api.vendor.commerce.service.CommerceProductCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/commerce/categories")
@RequiredArgsConstructor
public class CommerceProductCategoryController {

    private final CommerceProductCategoryService categoryService;
    private final LoggedInUserService loggedInUserService;

    /** Public: get root categories with children (for browse / product filters) */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommerceProductCategoryResponse>> getRootCategories(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(categoryService.getRootCategories(user.getCommunity().getId()));
    }

    /** Admin: get flat list of all active categories */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MANAGE_COMMERCE_PRODUCTS + "')")
    public ResponseEntity<List<CommerceProductCategoryResponse>> getAllCategories(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(categoryService.getAllCategories(user.getCommunity().getId()));
    }

    /** Admin: create a category */
    @PostMapping
    @PreAuthorize("hasAuthority('" + PermissionConstants.MANAGE_COMMERCE_PRODUCTS + "')")
    public ResponseEntity<CommerceProductCategoryResponse> create(
            @Valid @RequestBody CommerceProductCategoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.create(request, user.getCommunity().getId()));
    }

    /** Admin: update a category */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MANAGE_COMMERCE_PRODUCTS + "')")
    public ResponseEntity<CommerceProductCategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CommerceProductCategoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(categoryService.update(id, request, user.getCommunity().getId()));
    }

    /** Admin: deactivate a category */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermissionConstants.MANAGE_COMMERCE_PRODUCTS + "')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
