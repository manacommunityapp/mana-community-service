package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.DonationRequest;
import com.manacommunity.api.marketplace.dto.DonationResponse;
import com.manacommunity.api.marketplace.service.DonationService;
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
@RequestMapping("/api/marketplace/donations")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Page<DonationResponse>> getCommunityDonations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) {
            return ResponseEntity.ok(Page.empty());
        }
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(donationService.getCommunityDonations(communityId, pageable));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<List<DonationResponse>> getMyDonations(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(donationService.getMyDonations(user.getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Listing')")
    public ResponseEntity<DonationResponse> create(
            @Valid @RequestBody DonationRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(donationService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<DonationResponse> claimDonation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(donationService.claimDonation(id, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('Delete Listing', 'Manage Marketplace')")
    public ResponseEntity<Void> deleteDonation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        donationService.deleteDonation(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
