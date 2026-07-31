package com.manacommunity.api.food.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.food.service.FoodCloudKitchenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/food/cloud-kitchens")
@RequiredArgsConstructor
public class FoodCloudKitchenController {

    private final FoodCloudKitchenService cloudKitchenService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Food Cloud Kitchens')")
    public ResponseEntity<?> list(@AuthenticationPrincipal UserPrincipal principal,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(cloudKitchenService.list(communityId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Food Cloud Kitchens')")
    public ResponseEntity<?> getById(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(cloudKitchenService.getById(communityId, id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Manage Food Cloud Kitchens')")
    public ResponseEntity<?> create(@AuthenticationPrincipal UserPrincipal principal,
                                    @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(cloudKitchenService.create(communityId, request, user));
    }

    @GetMapping("/{id}/brands")
    @PreAuthorize("hasAuthority('View Food Cloud Kitchens')")
    public ResponseEntity<?> getBrands(@AuthenticationPrincipal UserPrincipal principal,
                                       @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(cloudKitchenService.getBrands(communityId, id));
    }

    @PostMapping("/{id}/brands")
    @PreAuthorize("hasAuthority('Manage Food Cloud Kitchens')")
    public ResponseEntity<?> createBrand(@AuthenticationPrincipal UserPrincipal principal,
                                         @PathVariable Long id,
                                         @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(cloudKitchenService.createBrand(communityId, id, request));
    }

    @GetMapping("/{id}/slots")
    @PreAuthorize("hasAuthority('View Food Cloud Kitchens')")
    public ResponseEntity<?> getSlots(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(cloudKitchenService.getSlots(communityId, id));
    }

    @GetMapping("/{id}/analytics")
    @PreAuthorize("hasAuthority('View Food Cloud Kitchens')")
    public ResponseEntity<?> getAnalytics(@AuthenticationPrincipal UserPrincipal principal,
                                           @PathVariable Long id,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(cloudKitchenService.getAnalytics(communityId, id, startDate, endDate));
    }
}
