package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.MarketOfferRequest;
import com.manacommunity.api.marketplace.dto.MarketOfferResponse;
import com.manacommunity.api.marketplace.service.MarketOfferService;
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

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping({"/api/marketplace/offers", "/api/v1/marketplace/offers"})
@RequiredArgsConstructor
public class MarketOfferController {

    private final MarketOfferService offerService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/received")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<List<MarketOfferResponse>> getReceivedOffers(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(offerService.getReceivedOffers(user.getId()));
    }

    @GetMapping("/sent")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<List<MarketOfferResponse>> getSentOffers(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(offerService.getSentOffers(user.getId()));
    }

    @GetMapping("/listing/{listingId}")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<List<MarketOfferResponse>> getOffersForListing(
            @PathVariable Long listingId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(offerService.getOffersForListing(listingId, user.getId()));
    }

    @PostMapping("/listing/{listingId}")
    @PreAuthorize("hasAuthority('Create Listing')")
    public ResponseEntity<MarketOfferResponse> makeOffer(
            @PathVariable Long listingId,
            @Valid @RequestBody MarketOfferRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(offerService.makeOffer(listingId, req, user));
    }

    @PutMapping("/{id}/respond")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketOfferResponse> respondToOffer(
            @PathVariable Long id,
            @RequestParam String action,
            @RequestParam(required = false) BigDecimal counterPrice,
            @RequestParam(required = false) String counterMessage,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(offerService.respondToOffer(id, action, counterPrice, counterMessage, user.getId()));
    }
}
