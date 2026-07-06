package com.manacommunity.api.retail.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.retail.dto.SupplierDto;
import com.manacommunity.api.retail.service.SupplierService;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/retail/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<SupplierDto>> getAllSuppliers(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(supplierService.getAllSuppliers(user.getCommunity().getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<SupplierDto> createSupplier(
            @RequestBody SupplierDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(supplierService.createSupplier(dto, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<SupplierDto> updateSupplier(
            @PathVariable Long id,
            @RequestBody SupplierDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Void> deleteSupplier(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.ok().build();
    }
}
