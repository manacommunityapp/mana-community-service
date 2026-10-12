package com.manacommunity.api.commerce.core.controller;

import com.manacommunity.api.commerce.core.dto.CommerceFlashSaleDto;
import com.manacommunity.api.commerce.core.dto.CommerceFlashSaleRequest;
import com.manacommunity.api.commerce.core.service.CommerceFlashSaleService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/commerce/flash-sales")
@RequiredArgsConstructor
public class CommerceFlashSaleController {

    private final CommerceFlashSaleService flashSaleService;
    private final LoggedInUserService loggedInUserService;

    /** All members can view active flash sales */
    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommerceFlashSaleDto>> getActive(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(flashSaleService.getActiveFlashSales(user));
    }

    /** Admin: list all flash sales */
    @GetMapping
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<List<CommerceFlashSaleDto>> getAll(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(flashSaleService.getAllFlashSales(user));
    }

    /** Admin: create a flash sale */
    @PostMapping
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<CommerceFlashSaleDto> create(
            @Valid @RequestBody CommerceFlashSaleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(flashSaleService.createFlashSale(user, request));
    }

    /** Admin: cancel a flash sale */
    @PostMapping("/{saleId}/cancel")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<CommerceFlashSaleDto> cancel(
            @PathVariable Long saleId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(flashSaleService.cancelFlashSale(user, saleId));
    }
}
