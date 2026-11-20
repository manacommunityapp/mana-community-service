package com.manacommunity.api.sportsops.controller;

import com.manacommunity.api.sportsops.entity.MerchandiseOrder;
import com.manacommunity.api.sportsops.entity.TournamentMerchandise;
import com.manacommunity.api.sportsops.service.MerchandiseService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sports-ops/merchandise")
@RequiredArgsConstructor
public class MerchandiseController {
    private final MerchandiseService service;

    @GetMapping
    public List<TournamentMerchandise> getItems(@RequestParam Long tournamentId) {
        return service.getItems(tournamentId);
    }

    @PostMapping
    public ResponseEntity<TournamentMerchandise> addItem(
            @RequestBody TournamentMerchandise item,
            @AuthenticationPrincipal UserPrincipal principal) {
        item.setCommunityId(principal.getCommunityId());
        return ResponseEntity.status(201).body(service.addItem(item));
    }

    @PostMapping("/{id}/order")
    public ResponseEntity<MerchandiseOrder> placeOrder(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        int qty = body.getOrDefault("quantity", 1);
        return ResponseEntity.status(201).body(
            service.placeOrder(id, principal.getUserId(), principal.getCommunityId(), qty));
    }

    @GetMapping("/my-orders")
    public List<MerchandiseOrder> myOrders(@AuthenticationPrincipal UserPrincipal principal) {
        return service.getOrdersForUser(principal.getUserId());
    }
}
