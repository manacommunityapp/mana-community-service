package com.manacommunity.api.controller;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.service.InventoryLotService;
import com.manacommunity.api.service.InventoryScrapService;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <pre>
 * ── Lots ──
 * GET    /api/inventory/lots/product/{productId}  — lots for a product
 * GET    /api/inventory/lots/{id}                 — single lot
 * POST   /api/inventory/lots                      — create lot/serial
 *
 * ── Scrap ──
 * GET    /api/inventory/scrap                     — all scrap records
 * GET    /api/inventory/scrap/product/{productId} — scraps for a product
 * POST   /api/inventory/scrap                     — quick scrap (create + confirm)
 * POST   /api/inventory/scrap/draft               — create DRAFT scrap
 * POST   /api/inventory/scrap/{id}/confirm        — confirm DRAFT → DONE
 * </pre>
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryLotScrapController {

    private final InventoryLotService lotService;
    private final InventoryScrapService scrapService;
    private final LoggedInUserService loggedInUserService;

    // ─── Lots ────────────────────────────────────────────────────────────

    @GetMapping("/lots/product/{productId}")
    public ResponseEntity<List<InventoryLotResponse>> getLotsByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(lotService.getByProduct(productId));
    }

    @GetMapping("/lots/{id}")
    public ResponseEntity<InventoryLotResponse> getLot(@PathVariable Long id) {
        return ResponseEntity.ok(lotService.getById(id));
    }

    @PostMapping("/lots")
    public ResponseEntity<InventoryLotResponse> createLot(@Valid @RequestBody InventoryLotRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lotService.create(req));
    }

    // ─── Scrap ───────────────────────────────────────────────────────────

    @GetMapping("/scrap")
    public ResponseEntity<List<InventoryScrapResponse>> getAllScrap() {
        return ResponseEntity.ok(scrapService.getAll());
    }

    @GetMapping("/scrap/product/{productId}")
    public ResponseEntity<List<InventoryScrapResponse>> getScrapByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(scrapService.getByProduct(productId));
    }

    /**
     * Quick scrap — matches the existing frontend flow in DoubleEntryInventory.tsx
     * which calls stockService.scrapStock() as a single-step operation.
     */
    @PostMapping("/scrap")
    public ResponseEntity<InventoryScrapResponse> quickScrap(
            @Valid @RequestBody InventoryScrapRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(scrapService.quickScrap(req, user.getId()));
    }

    /** Create a DRAFT scrap (two-step flow). */
    @PostMapping("/scrap/draft")
    public ResponseEntity<InventoryScrapResponse> createDraftScrap(
            @Valid @RequestBody InventoryScrapRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scrapService.createScrap(req));
    }

    /** Confirm a DRAFT scrap → actually moves stock to scrap location. */
    @PostMapping("/scrap/{id}/confirm")
    public ResponseEntity<InventoryScrapResponse> confirmScrap(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(scrapService.confirmScrap(id, user.getId()));
    }
}
