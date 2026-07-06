package com.manacommunity.api.retail.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.retail.dto.RetailOrderDto;
import com.manacommunity.api.retail.service.RetailOrderService;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/retail/orders")
@RequiredArgsConstructor
public class RetailOrderController {

    private final RetailOrderService orderService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<List<RetailOrderDto>> getOrders(
            @RequestParam(required = false) String type,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(orderService.getOrders(user.getCommunity().getId(), type));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<RetailOrderDto> createOrder(
            @RequestBody RetailOrderDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(orderService.createOrder(dto, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<RetailOrderDto> updateOrder(
            @PathVariable Long id,
            @RequestBody RetailOrderDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(orderService.updateOrder(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('View Admin')")
    public ResponseEntity<Void> deleteOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok().build();
    }
}
