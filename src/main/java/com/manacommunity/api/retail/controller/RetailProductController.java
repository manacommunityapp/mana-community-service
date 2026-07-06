package com.manacommunity.api.retail.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.retail.dto.RetailProductDto;
import com.manacommunity.api.retail.service.RetailProductService;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/retail/products")
@RequiredArgsConstructor
public class RetailProductController {

    private final RetailProductService productService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<RetailProductDto>> getAllProducts(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(productService.getAllProducts(user.getCommunity().getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<RetailProductDto> createProduct(
            @RequestBody RetailProductDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(productService.createProduct(dto, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<RetailProductDto> updateProduct(
            @PathVariable Long id,
            @RequestBody RetailProductDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(productService.updateProduct(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        productService.deleteProduct(id);
        return ResponseEntity.ok().build();
    }
}
