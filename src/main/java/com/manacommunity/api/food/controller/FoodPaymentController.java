package com.manacommunity.api.food.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.food.service.FoodPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/food/payments")
@RequiredArgsConstructor
public class FoodPaymentController {

    private final FoodPaymentService paymentService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Food Payments')")
    public ResponseEntity<?> getMyPayments(@AuthenticationPrincipal UserPrincipal principal,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(paymentService.getMyPayments(communityId, user.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/wallet")
    @PreAuthorize("hasAuthority('View Food Payments')")
    public ResponseEntity<?> getWallet(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(paymentService.getWallet(communityId, user.getId()));
    }

    @PostMapping("/wallet/credit")
    @PreAuthorize("hasAuthority('Manage Food Payments')")
    public ResponseEntity<?> credit(@AuthenticationPrincipal UserPrincipal principal,
                                    @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        Long userId = Long.valueOf(request.get("userId").toString());
        BigDecimal amount = new BigDecimal(request.get("amount").toString());
        String referenceType = (String) request.get("referenceType");
        Long referenceId = Long.valueOf(request.get("referenceId").toString());
        return ResponseEntity.ok(paymentService.credit(communityId, userId, amount, referenceType, referenceId));
    }

    @GetMapping("/wallet/transactions")
    @PreAuthorize("hasAuthority('View Food Payments')")
    public ResponseEntity<?> getTransactions(@AuthenticationPrincipal UserPrincipal principal,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(paymentService.getTransactions(communityId, user.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }
}
