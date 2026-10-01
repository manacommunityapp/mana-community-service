package com.manacommunity.api.cfbos.wallet.controller;

import com.manacommunity.api.cfbos.wallet.dto.*;
import com.manacommunity.api.cfbos.wallet.entity.SecurityDeposit;
import com.manacommunity.api.cfbos.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cfbos/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/resident/{residentId}")
    public ResponseEntity<WalletDto> getWallet(@PathVariable Long residentId) {
        return ResponseEntity.ok(walletService.getWallet(residentId));
    }

    @PostMapping("/topup")
    public ResponseEntity<WalletTransactionDto> topup(@RequestBody TopupWalletRequest request) {
        return ResponseEntity.ok(walletService.topupWallet(request));
    }

    @GetMapping("/resident/{residentId}/passbook")
    public ResponseEntity<List<WalletTransactionDto>> getPassbook(@PathVariable Long residentId) {
        return ResponseEntity.ok(walletService.getPassbook(residentId));
    }

    @GetMapping("/resident/{residentId}/deposits")
    public ResponseEntity<List<SecurityDeposit>> getDeposits(@PathVariable Long residentId) {
        return ResponseEntity.ok(walletService.getSecurityDeposits(residentId));
    }

    @PostMapping("/deposits")
    public ResponseEntity<SecurityDeposit> createDeposit(@RequestBody SecurityDeposit deposit) {
        return ResponseEntity.ok(walletService.saveSecurityDeposit(deposit));
    }
}
