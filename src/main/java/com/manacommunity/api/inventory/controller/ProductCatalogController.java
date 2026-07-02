package com.manacommunity.api.inventory.controller;

import com.manacommunity.api.inventory.dto.ProductDto;
import com.manacommunity.api.inventory.service.ProductCatalogService;
import com.manacommunity.api.security.UserPrincipal;
import com.manacommunity.api.service.PermissionCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import static com.manacommunity.api.constants.PermissionConstants.VIEW_ADMIN;

@RestController
@RequestMapping("/api/inventory/products")
@RequiredArgsConstructor
public class ProductCatalogController {

    private final ProductCatalogService catalogService;
    private final PermissionCheckService permissionCheckService;

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts(
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(catalogService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getProductById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(catalogService.getProductById(id));
    }

    @PostMapping
    public ResponseEntity<ProductDto> createProduct(
            @RequestBody ProductDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(catalogService.createProduct(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        return ResponseEntity.ok(catalogService.updateProduct(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        permissionCheckService.requireAnyPermission(principal, VIEW_ADMIN);
        catalogService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }
}
