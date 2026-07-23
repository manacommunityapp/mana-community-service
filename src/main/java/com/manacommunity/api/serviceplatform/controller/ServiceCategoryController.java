package com.manacommunity.api.serviceplatform.controller;

import com.manacommunity.api.serviceplatform.dto.request.CreateServiceCategoryRequest;
import com.manacommunity.api.serviceplatform.dto.response.ServiceCategoryResponse;
import com.manacommunity.api.serviceplatform.service.ServiceCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-platform/categories")
@RequiredArgsConstructor
public class ServiceCategoryController {

    private final ServiceCatalogService catalogService;

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
            @Valid @RequestBody CreateServiceCategoryRequest request) {
        return ResponseEntity.ok(catalogService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Service Catalog')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
