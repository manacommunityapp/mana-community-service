package com.manacommunity.api.finance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/v1/gold")
public class GoldController {

    record GoldPriceDto(double per_gram_22k, double per_gram_24k, double change_pct_24h, String updated_at) {}
    record GoldHoldingDto(String id, double grams, String purity, double avg_buy_price,
                          double current_value, double unrealised_pnl, double unrealised_pnl_pct) {}
    record GoldTxDto(String id, String type, double grams, double price_per_gram,
                     double amount, String purity, String status, String created_at) {}
    record BuyRequest(double amount_inr, String purity) {}
    record SellRequest(double grams, String purity) {}
    record BuyResult(String upi_intent_url, String phone_pe_url, String transaction_ref, double grams, double amount) {}
    record SellResult(String transaction_ref, double amount) {}

    private static final double BASE_24K = 7450.0;
    private static final double BASE_22K = 6850.0;

    @GetMapping("/price")
    public ResponseEntity<GoldPriceDto> getPrice() {
        double jitter = (ThreadLocalRandom.current().nextDouble() - 0.5) * 100;
        double change = Math.round((jitter / BASE_24K) * 10000.0) / 100.0;
        return ResponseEntity.ok(new GoldPriceDto(
            BASE_22K + jitter * 0.92,
            BASE_24K + jitter,
            change,
            Instant.now().toString()
        ));
    }

    @GetMapping("/holdings")
    public ResponseEntity<List<GoldHoldingDto>> getHoldings() {
        double currentNav = BASE_24K;
        double avgBuy = 7100.0;
        double grams = 2.5;
        double currentValue = grams * currentNav;
        double pnl = (currentNav - avgBuy) * grams;
        double pnlPct = (currentNav - avgBuy) / avgBuy * 100;
        return ResponseEntity.ok(List.of(
            new GoldHoldingDto("holding-1", grams, "24K", avgBuy, currentValue, pnl, pnlPct)
        ));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<GoldTxDto>> getTransactions() {
        return ResponseEntity.ok(List.of(
            new GoldTxDto("gt-1", "BUY", 1.5, 7200.0, 10800.0, "24K", "COMPLETED",
                Instant.now().minusSeconds(15 * 86400L).toString()),
            new GoldTxDto("gt-2", "BUY", 1.0, 6950.0, 6950.0, "24K", "COMPLETED",
                Instant.now().minusSeconds(45 * 86400L).toString())
        ));
    }

    @PostMapping("/buy")
    public ResponseEntity<BuyResult> buy(@RequestBody BuyRequest req) {
        double price = "24K".equals(req.purity()) ? BASE_24K : BASE_22K;
        double grams = Math.round((req.amount_inr() / price) * 10000.0) / 10000.0;
        String tr = "MANA-GOLD-" + System.currentTimeMillis();
        String pa = "gold@setu";
        String pn = "Mana+Digital+Gold";
        return ResponseEntity.ok(new BuyResult(
            String.format("upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tr=%s&tn=Digital+Gold+Purchase", pa, pn, req.amount_inr(), tr),
            String.format("phonepe://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tr=%s", pa, pn, req.amount_inr(), tr),
            tr, grams, req.amount_inr()
        ));
    }

    @PostMapping("/sell")
    public ResponseEntity<SellResult> sell(@RequestBody SellRequest req) {
        double price = "24K".equals(req.purity()) ? BASE_24K : BASE_22K;
        double amount = Math.round(req.grams() * price);
        return ResponseEntity.ok(new SellResult("MANA-GOLD-SELL-" + System.currentTimeMillis(), amount));
    }
}
