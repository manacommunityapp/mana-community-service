package com.manacommunity.api.vendor.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.vendor.dto.RatingRequest;
import com.manacommunity.api.vendor.dto.RatingResponse;
import com.manacommunity.api.vendor.service.VmsRatingService;
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

@RestController
@RequestMapping("/api/vendor/ratings")
@RequiredArgsConstructor
public class VmsRatingController {

    private final VmsRatingService ratingService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasAuthority('View Vendor Management')")
    public ResponseEntity<Page<RatingResponse>> getVendorRatings(
            @PathVariable Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ratingService.getVendorRatings(vendorId, user.getCommunity().getId(), pageable));
    }

    @PutMapping("/{id}/reply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RatingResponse> reply(@PathVariable Long id, @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ratingService.replyToRating(id, body.get("reply"), user));
    }

    @PutMapping("/{id}/moderate")
    @PreAuthorize("hasAuthority('Manage Vendors')")
    public ResponseEntity<RatingResponse> moderate(@PathVariable Long id, @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(ratingService.moderate(id, body.get("status"), user.getCommunity().getId()));
    }

    @PostMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> report(@PathVariable Long id, @RequestBody java.util.Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        ratingService.report(id, body.getOrDefault("reason", ""), loggedInUserService.resolve(principal));
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Rate Vendor')")
    public ResponseEntity<RatingResponse> submitRating(
            @Valid @RequestBody RatingRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ratingService.submitRating(req, user, user.getCommunity()));
    }
}
