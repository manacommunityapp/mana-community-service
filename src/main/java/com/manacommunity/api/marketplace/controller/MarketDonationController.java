package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.MarketDonationRequest;
import com.manacommunity.api.marketplace.dto.MarketDonationResponse;
import com.manacommunity.api.marketplace.entity.MarketDonation;
import com.manacommunity.api.marketplace.service.MarketDonationService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
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
@RequestMapping({"/api/marketplace/donations", "/api/v1/marketplace/donations"})
@RequiredArgsConstructor
public class MarketDonationController {

    private final MarketDonationService donationService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Page<MarketDonationResponse>> getDonations(
            @RequestParam(required = false) MarketDonation.SharingMode mode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(Page.empty());
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(donationService.getCommunityDonations(communityId, mode, pageable));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<List<MarketDonationResponse>> getMyDonations(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(donationService.getMyDonations(user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketDonationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(donationService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Listing')")
    public ResponseEntity<MarketDonationResponse> create(
            @Valid @RequestBody MarketDonationRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationService.create(req, user, user.getCommunity()));
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketDonationResponse> claim(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(donationService.claim(id, user));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('Create Listing')")
    public ResponseEntity<MarketDonationResponse> markDonated(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(donationService.markDonated(id, user.getId()));
    }
}
