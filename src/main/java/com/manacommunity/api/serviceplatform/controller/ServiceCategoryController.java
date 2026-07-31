package com.manacommunity.api.serviceplatform.controller;

import com.manacommunity.api.serviceplatform.dto.request.CreateServiceCategoryRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceCategoryResponse;
import com.manacommunity.api.serviceplatform.service.ServiceCatalogService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-platform/categories")
@RequiredArgsConstructor
public class ServiceCategoryController {

    private final ServiceCatalogService catalogService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<List<ServiceCategoryResponse>> listCategories(
            @RequestParam Long domainId) {
        return ResponseEntity.ok(catalogService.listCategories(domainId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Service Catalog')")
    public ResponseEntity<ServiceCategoryResponse> getCategory(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.getCategory(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Manage Service Catalog')")
    public ResponseEntity<ServiceCategoryResponse> createCategory(
            @Valid @RequestBody CreateServiceCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(catalogService.createCategory(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Service Catalog')")
    public ResponseEntity<ServiceCategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateServiceCategoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(catalogService.updateCategory(id, request, communityId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Service Catalog')")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        catalogService.deleteCategory(id, communityId);
        return ResponseEntity.noContent().build();
    }
}
