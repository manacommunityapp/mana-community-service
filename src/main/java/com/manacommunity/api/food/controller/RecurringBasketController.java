package com.manacommunity.api.food.controller;

import com.manacommunity.api.food.entity.RecurringBasket;
import com.manacommunity.api.food.entity.RecurringBasketItem;
import com.manacommunity.api.food.service.RecurringBasketService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/food/baskets")
@RequiredArgsConstructor
public class RecurringBasketController {

    private final RecurringBasketService basketService;

    @GetMapping
    public ResponseEntity<List<RecurringBasket>> getMyBaskets(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(basketService.getBaskets(principal.getUserId()));
    }

    @PostMapping
    public ResponseEntity<RecurringBasket> createBasket(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        RecurringBasket basket = new RecurringBasket();
        basket.setCommunityId(principal.getCommunityId());
        basket.setUserId(principal.getUserId());
        basket.setVendorId(Long.parseLong(body.get("vendorId").toString()));
        basket.setName(body.get("name").toString());
        basket.setFrequency(RecurringBasket.BasketFrequency.valueOf(body.get("frequency").toString()));
        basket.setNextOrderDate(java.time.LocalDate.parse(body.get("nextOrderDate").toString()));
        @SuppressWarnings("unchecked")
        List<RecurringBasketItem> items = (List<RecurringBasketItem>) body.getOrDefault("items", List.of());
        return ResponseEntity.status(201).body(basketService.createBasket(basket, items));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringBasket> updateBasket(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        RecurringBasket updated = new RecurringBasket();
        updated.setName(body.get("name").toString());
        updated.setFrequency(RecurringBasket.BasketFrequency.valueOf(body.get("frequency").toString()));
        updated.setNextOrderDate(java.time.LocalDate.parse(body.get("nextOrderDate").toString()));
        @SuppressWarnings("unchecked")
        List<RecurringBasketItem> items = (List<RecurringBasketItem>) body.getOrDefault("items", List.of());
        return ResponseEntity.ok(basketService.updateBasket(id, principal.getUserId(), updated, items));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Void> pause(@PathVariable Long id) {
        basketService.pauseBasket(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Void> resume(@PathVariable Long id) {
        basketService.resumeBasket(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        basketService.cancelBasket(id);
        return ResponseEntity.noContent().build();
    }
}
