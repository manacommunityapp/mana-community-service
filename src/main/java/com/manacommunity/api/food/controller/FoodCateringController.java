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
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Catering', 'Manage Food Catering')")
    public ResponseEntity<?> list(@AuthenticationPrincipal UserPrincipal principal,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(cateringService.list(communityId, PageRequest.of(page, size)));
    }

    @GetMapping("/caterers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Catering', 'Manage Food Catering')")
    public ResponseEntity<?> getById(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(cateringService.getById(communityId, id));
    }

    @PostMapping({"/caterers/register", "/caterers"})
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN') or hasAnyAuthority('Manage Food Catering')")
    public ResponseEntity<?> register(@AuthenticationPrincipal UserPrincipal principal,
                                      @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(cateringService.register(communityId, request, user));
    }

    @GetMapping("/caterers/{id}/packages")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Catering', 'Manage Food Catering')")
    public ResponseEntity<?> getPackages(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(cateringService.getPackages(communityId, id));
    }

    @PostMapping("/requests")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Catering', 'Manage Food Catering')")
    public ResponseEntity<?> createRequest(@AuthenticationPrincipal UserPrincipal principal,
                                           @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(cateringService.createRequest(communityId, request, user));
    }

    @GetMapping({"/requests", "/requests/mine"})
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Catering', 'Manage Food Catering')")
    public ResponseEntity<?> getMyRequests(@AuthenticationPrincipal UserPrincipal principal,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(cateringService.getMyRequests(communityId, user != null ? user.getId() : null, PageRequest.of(page, size)));
    }

    @PostMapping("/quotations")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN') or hasAnyAuthority('Manage Food Catering')")
    public ResponseEntity<?> submitQuotation(@AuthenticationPrincipal UserPrincipal principal,
                                             @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(cateringService.submitQuotation(communityId, request));
    }

    @PostMapping("/requests/{requestId}/quotations")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN') or hasAnyAuthority('Manage Food Catering')")
    public ResponseEntity<?> submitQuotationForRequest(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable Long requestId,
                                                       @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        request.put("requestId", requestId);
        return ResponseEntity.ok(cateringService.submitQuotation(communityId, request));
    }

    @GetMapping("/requests/{requestId}/quotations")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Catering', 'Manage Food Catering')")
    public ResponseEntity<?> getQuotations(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long requestId) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(cateringService.getQuotations(communityId, requestId));
    }

    @PostMapping("/quotations/{id}/accept")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Catering', 'Manage Food Catering')")
    public ResponseEntity<?> acceptQuotation(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(cateringService.acceptQuotation(communityId, id, user));
    }

    @PutMapping("/requests/{requestId}/quotations/{quotationId}/accept")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Catering', 'Manage Food Catering')")
    public ResponseEntity<?> acceptQuotationForRequest(@AuthenticationPrincipal UserPrincipal principal,
                                                       @PathVariable Long requestId,
                                                       @PathVariable Long quotationId) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(cateringService.acceptQuotation(communityId, quotationId, user));
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Catering', 'Manage Food Catering')")
    public ResponseEntity<?> getOrders(@AuthenticationPrincipal UserPrincipal principal,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(cateringService.getOrders(communityId, PageRequest.of(page, size)));
    }
}
