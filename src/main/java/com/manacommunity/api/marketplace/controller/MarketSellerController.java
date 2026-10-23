package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.MarketSellerDtos.*;
import com.manacommunity.api.marketplace.service.MarketSellerService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/marketplace/sellers", "/api/v1/marketplace/sellers"})
@RequiredArgsConstructor
public class MarketSellerController {

    private final MarketSellerService sellerService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping("/register")
    public ResponseEntity<SellerProfileResponse> registerSeller(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SellerRegisterRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sellerService.registerSeller(user, request));
    }

    @PostMapping("/kyc/submit")
    public ResponseEntity<SellerProfileResponse> submitKyc(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SellerKycSubmitRequest request) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sellerService.submitKyc(user, request));
    }

    @GetMapping("/me")
    public ResponseEntity<SellerProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sellerService.getMyProfile(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SellerProfileResponse> getSellerById(@PathVariable Long id) {
        return ResponseEntity.ok(sellerService.getSellerById(id));
    }

    @GetMapping("/community/{communityId}")
    public ResponseEntity<Page<SellerProfileResponse>> getCommunitySellers(
            @PathVariable Long communityId,
            Pageable pageable) {
        return ResponseEntity.ok(sellerService.getCommunitySellers(communityId, pageable));
    }

    @PutMapping("/status/open")
    public ResponseEntity<SellerProfileResponse> toggleStoreOpen(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam boolean isOpen) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(sellerService.toggleStoreOpen(user, isOpen));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @PostMapping("/{sellerId}/kyc/decision")
    public ResponseEntity<SellerProfileResponse> processKycDecision(
            @PathVariable Long sellerId,
            @Valid @RequestBody KycDecisionRequest request) {
        return ResponseEntity.ok(sellerService.processKycDecision(sellerId, request));
    }

    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    @GetMapping("/community/{communityId}/kyc/pending")
    public ResponseEntity<Page<SellerProfileResponse>> getPendingKycSellers(
            @PathVariable Long communityId,
            Pageable pageable) {
        return ResponseEntity.ok(sellerService.getPendingKycSellers(communityId, pageable));
    }
}
