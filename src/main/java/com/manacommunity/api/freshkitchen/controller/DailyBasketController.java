package com.manacommunity.api.freshkitchen.controller;

import com.manacommunity.api.freshkitchen.dto.*;
import com.manacommunity.api.freshkitchen.service.DailyBasketService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping({"/api/v1/daily-basket", "/daily-basket"})
@RequiredArgsConstructor
public class DailyBasketController {

    private final DailyBasketService dailyBasketService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<DailyBasketDto> getMyBasket(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        return ResponseEntity.ok(dailyBasketService.getOrCreateUserBasket(user.getId(), communityId));
    }

    @PutMapping
    public ResponseEntity<DailyBasketDto> updateBasketConfig(
            @RequestBody DailyBasketDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        return ResponseEntity.ok(dailyBasketService.updateBasketConfig(user.getId(), communityId, dto));
    }

    @PostMapping("/items")
    public ResponseEntity<DailyBasketItemDto> addItem(
            @Valid @RequestBody BasketItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        return ResponseEntity.ok(dailyBasketService.addItem(user.getId(), communityId, request));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<DailyBasketItemDto> updateItem(
            @PathVariable Long itemId,
            @Valid @RequestBody BasketItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        return ResponseEntity.ok(dailyBasketService.updateItem(user.getId(), communityId, itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        dailyBasketService.removeItem(user.getId(), communityId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/pause")
    public ResponseEntity<DailyBasketDto> pauseBasket(
            @Valid @RequestBody VacationPauseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        return ResponseEntity.ok(dailyBasketService.pauseBasket(user.getId(), communityId, request));
    }

    @PostMapping("/resume")
    public ResponseEntity<DailyBasketDto> resumeBasket(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        return ResponseEntity.ok(dailyBasketService.resumeBasket(user.getId(), communityId));
    }

    @GetMapping("/today-pass")
    public ResponseEntity<DeliveryPassDto> getTodayPass(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : 1L;
        DeliveryPassDto pass = dailyBasketService.getTodayDeliveryPass(user.getId(), communityId, date);
        return pass != null ? ResponseEntity.ok(pass) : ResponseEntity.noContent().build();
    }

    @GetMapping("/orders/lookup")
    public ResponseEntity<DeliveryPassDto> lookupOrder(@RequestParam String query) {
        return ResponseEntity.ok(dailyBasketService.lookupOrderByTokenOrNumber(query));
    }

    @PostMapping("/handover/verify")
    public ResponseEntity<DeliveryPassDto> verifyHandover(@Valid @RequestBody HandoverVerifyRequest request) {
        return ResponseEntity.ok(dailyBasketService.verifyDeliveryHandover(request));
    }
}
