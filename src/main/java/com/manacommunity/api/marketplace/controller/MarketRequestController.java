package com.manacommunity.api.marketplace.controller;

import com.manacommunity.api.marketplace.dto.MarketProductRequestDto;
import com.manacommunity.api.marketplace.dto.MarketRequestOfferDto;
import com.manacommunity.api.marketplace.service.MarketProductRequestService;
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
@RequestMapping({"/api/marketplace/requests", "/api/v1/marketplace/requests"})
@RequiredArgsConstructor
public class MarketRequestController {

    private final MarketProductRequestService requestService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<Page<MarketProductRequestDto.Response>> getRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(Page.empty());
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(requestService.getActiveRequests(communityId, pageable));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<List<MarketProductRequestDto.Response>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(requestService.getMyRequests(user.getId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Marketplace')")
    public ResponseEntity<MarketProductRequestDto.Response> getById(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Listing')")
    public ResponseEntity<MarketProductRequestDto.Response> create(
            @Valid @RequestBody MarketProductRequestDto.Request req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requestService.create(req, user, user.getCommunity()));
    }

    @PostMapping("/{id}/offers")
    @PreAuthorize("hasAuthority('Create Listing')")
    public ResponseEntity<MarketRequestOfferDto.Response> makeOffer(
            @PathVariable Long id,
            @Valid @RequestBody MarketRequestOfferDto.Request req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(requestService.makeOffer(id, req, user));
    }
}
