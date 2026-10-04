package com.manacommunity.api.groupbuying.controller;

import com.manacommunity.api.groupbuying.dto.*;
import com.manacommunity.api.groupbuying.service.GroupBuyingService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group-buying")
@RequiredArgsConstructor
public class GroupBuyingController {

    private final GroupBuyingService groupBuyingService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/deals")
    public ResponseEntity<List<GroupDealResponse>> getDeals(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(groupBuyingService.getDeals(communityId));
    }

    @GetMapping("/deals/{id}")
    public ResponseEntity<GroupDealResponse> getDealById(@PathVariable Long id) {
        return ResponseEntity.ok(groupBuyingService.getDealById(id));
    }

    @GetMapping("/deals/almost-unlocked")
    public ResponseEntity<List<GroupDealResponse>> getAlmostUnlocked(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupBuyingService.getAlmostUnlocked(user.getCommunity().getId()));
    }

    @GetMapping("/deals/featured")
    public ResponseEntity<List<GroupDealResponse>> getFeaturedDeals(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupBuyingService.getFeaturedDeals(user.getCommunity().getId()));
    }

    @GetMapping("/deals/festival")
    public ResponseEntity<List<GroupDealResponse>> getFestivalDeals(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupBuyingService.getFestivalDeals(user.getCommunity().getId()));
    }

    @PostMapping("/deals/{id}/join")
    public ResponseEntity<GroupOrderResponse> joinDeal(
            @PathVariable Long id,
            @Valid @RequestBody JoinDealRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupBuyingService.joinDeal(id, user, request));
    }

    @PostMapping("/orders/verify-pickup")
    public ResponseEntity<PickupVerificationResponse> verifyPickup(
            @Valid @RequestBody PickupVerificationRequest request) {
        return ResponseEntity.ok(groupBuyingService.verifyPickupPass(request));
    }

    @PostMapping("/vendor/deals")
    public ResponseEntity<GroupDealResponse> createVendorDeal(
            @Valid @RequestBody CreateVendorDealRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupBuyingService.createVendorDeal(user.getCommunity().getId(), user, request));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<GroupOrderResponse>> getMyOrders(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupBuyingService.getUserOrders(user.getId()));
    }

    @GetMapping("/demand")
    public ResponseEntity<List<DemandResponse>> getDemandBoard(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupBuyingService.getDemandBoard(user.getCommunity().getId()));
    }

    @PostMapping("/demand")
    public ResponseEntity<DemandResponse> createDemand(
            @Valid @RequestBody CreateDemandRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupBuyingService.createDemand(user.getCommunity().getId(), user, request));
    }

    @PostMapping("/demand/{id}/upvote")
    public ResponseEntity<Void> upvoteDemand(@PathVariable Long id) {
        groupBuyingService.upvoteDemand(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/community-savings")
    public ResponseEntity<CommunitySavingsResponse> getCommunitySavings(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupBuyingService.getCommunitySavings(user.getCommunity().getId()));
    }

    @PostMapping("/demand/{id}/offers")
    public ResponseEntity<VendorOfferDto> submitVendorOffer(
            @PathVariable Long id,
            @Valid @RequestBody SubmitVendorOfferRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupBuyingService.submitVendorOffer(id, user, request));
    }

    @PostMapping("/demand/{id}/offers/{offerId}/accept")
    public ResponseEntity<GroupDealResponse> acceptVendorOffer(
            @PathVariable Long id,
            @PathVariable Long offerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(groupBuyingService.acceptVendorOffer(id, offerId, user));
    }

}
