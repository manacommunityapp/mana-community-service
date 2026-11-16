package com.manacommunity.api.auctions.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Resident-facing auction marketplace (community-run item auctions, charity bids).
 * Distinct from /api/auction/* (sports player auction) and /api/events/auction-items (event fundraiser).
 */
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> listAuctions(
            @RequestParam(required = false) Long communityId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of(
                "content", List.of(),
                "totalElements", 0,
                "totalPages", 0,
                "number", page
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAuction(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of(
                "id", id,
                "status", "ACTIVE",
                "bids", List.of(),
                "watchCount", 0
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createAuction(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "id", System.currentTimeMillis(),
                "status", "DRAFT"
        ));
    }

    // ── Bids ──────────────────────────────────────────────────────────────────

    @GetMapping("/{auctionId}/bids")
    public ResponseEntity<Map<String, Object>> getBids(
            @PathVariable Long auctionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of(
                "content", List.of(),
                "totalElements", 0,
                "totalPages", 0,
                "number", page
        ));
    }

    @PostMapping("/{auctionId}/bids")
    public ResponseEntity<Map<String, Object>> placeBid(
            @PathVariable Long auctionId,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "id", System.currentTimeMillis(),
                "auctionId", auctionId,
                "amount", body.getOrDefault("amount", 0),
                "status", "ACCEPTED"
        ));
    }

    // ── Watchlist ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/watch")
    public ResponseEntity<Map<String, Object>> watchAuction(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true, "auctionId", id));
    }

    @DeleteMapping("/{id}/watch")
    public ResponseEntity<Map<String, Object>> unwatchAuction(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("success", true, "auctionId", id));
    }

    // ── User views ────────────────────────────────────────────────────────────

    @GetMapping("/my")
    public ResponseEntity<Map<String, Object>> myAuctions(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of(
                "content", List.of(),
                "totalElements", 0,
                "totalPages", 0,
                "number", page
        ));
    }

    @GetMapping("/my-bids")
    public ResponseEntity<Map<String, Object>> myBids(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of(
                "content", List.of(),
                "totalElements", 0,
                "totalPages", 0,
                "number", page
        ));
    }
}
