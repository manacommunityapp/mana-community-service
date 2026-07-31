package com.manacommunity.api.food.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.food.service.FoodCateringService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/food/catering")
@RequiredArgsConstructor
public class FoodCateringController {

    private final FoodCateringService cateringService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/caterers")
    @PreAuthorize("hasAuthority('View Food Catering')")
    public ResponseEntity<?> list(@AuthenticationPrincipal UserPrincipal principal,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(cateringService.list(communityId, PageRequest.of(page, size)));
    }

    @GetMapping("/caterers/{id}")
    @PreAuthorize("hasAuthority('View Food Catering')")
    public ResponseEntity<?> getById(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(cateringService.getById(communityId, id));
    }

    @PostMapping("/caterers/register")
    @PreAuthorize("hasAuthority('Manage Food Catering')")
    public ResponseEntity<?> register(@AuthenticationPrincipal UserPrincipal principal,
                                      @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(cateringService.register(communityId, request));
    }

    @GetMapping("/caterers/{id}/packages")
    @PreAuthorize("hasAuthority('View Food Catering')")
    public ResponseEntity<?> getPackages(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(cateringService.getPackages(communityId, id));
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAuthority('Manage Food Catering')")
    public ResponseEntity<?> createRequest(@AuthenticationPrincipal UserPrincipal principal,
                                           @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(cateringService.createRequest(communityId, request));
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('View Food Catering')")
    public ResponseEntity<?> getMyRequests(@AuthenticationPrincipal UserPrincipal principal,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(cateringService.getMyRequests(communityId, user.getId(), PageRequest.of(page, size)));
    }

    @PostMapping("/quotations")
    @PreAuthorize("hasAuthority('Manage Food Catering')")
    public ResponseEntity<?> submitQuotation(@AuthenticationPrincipal UserPrincipal principal,
                                             @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(cateringService.submitQuotation(communityId, request));
    }

    @GetMapping("/requests/{requestId}/quotations")
    @PreAuthorize("hasAuthority('View Food Catering')")
    public ResponseEntity<?> getQuotations(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long requestId) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(cateringService.getQuotations(communityId, requestId));
    }

    @PostMapping("/quotations/{id}/accept")
    @PreAuthorize("hasAuthority('Manage Food Catering')")
    public ResponseEntity<?> acceptQuotation(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(cateringService.acceptQuotation(communityId, id));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('View Food Catering')")
    public ResponseEntity<?> getOrders(@AuthenticationPrincipal UserPrincipal principal,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(cateringService.getOrders(communityId, PageRequest.of(page, size)));
    }
}
