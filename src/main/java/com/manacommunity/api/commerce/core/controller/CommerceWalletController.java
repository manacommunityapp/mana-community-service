package com.manacommunity.api.commerce.core.controller;

import com.manacommunity.api.commerce.core.dto.CommerceWalletDto;
import com.manacommunity.api.commerce.core.service.CommerceWalletService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/commerce/wallet")
@RequiredArgsConstructor
public class CommerceWalletController {

    private final CommerceWalletService walletService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    public ResponseEntity<CommerceWalletDto> getWallet(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(walletService.getOrCreateWallet(user));
    }

    @PostMapping("/topup")
    public ResponseEntity<CommerceWalletDto> topUp(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String ref = body.getOrDefault("referenceId", "MANUAL").toString();
        return ResponseEntity.ok(walletService.topUp(user, amount, ref));
    }

    @PostMapping("/debit")
    public ResponseEntity<CommerceWalletDto> debit(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String ref = body.getOrDefault("referenceId", "").toString();
        String desc = body.getOrDefault("description", "Wallet payment").toString();
        return ResponseEntity.ok(walletService.debit(user, amount, ref, desc));
    }
}
