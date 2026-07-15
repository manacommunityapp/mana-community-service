package com.manacommunity.api.controller;

import com.manacommunity.api.dto.*;
import com.manacommunity.api.service.InventoryPickingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * <pre>
 * GET    /api/inventory/warehouses/operation-types/stats  — dashboard cards
 * GET    /api/inventory/pickings                          — list all pickings
 * GET    /api/inventory/pickings/{id}                     — single picking
 * POST   /api/inventory/pickings                          — create picking
 * POST   /api/inventory/pickings/{id}/confirm             — DRAFT → CONFIRMED
 * POST   /api/inventory/pickings/{id}/validate            — CONFIRMED → DONE (double-entry move)
 * POST   /api/inventory/pickings/scrap                    — scrap stock
 * GET    /api/inventory/reporting/stock                   — stock level report
 * GET    /api/inventory/reporting/moves-history           — move history report
 * </pre>
 *
 * Matches the frontend stockService.ts endpoints exactly.
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryPickingController {

    private final InventoryPickingService pickingService;

    // ─── Dashboard stats ─────────────────────────────────────────────────

    @GetMapping("/warehouses/operation-types/stats")
    public ResponseEntity<List<PickingTypeStatsResponse>> getStats() {
        return ResponseEntity.ok(pickingService.getPickingTypeStats());
    }

    // ─── Pickings CRUD ───────────────────────────────────────────────────

    @GetMapping("/pickings")
    public ResponseEntity<List<PickingResponse>> getAllPickings() {
        return ResponseEntity.ok(pickingService.getAllPickings());
    }

    @GetMapping("/pickings/{id}")
    public ResponseEntity<PickingResponse> getPicking(@PathVariable Long id) {
        return ResponseEntity.ok(pickingService.getPickingById(id));
    }

    @PostMapping("/pickings")
    public ResponseEntity<PickingResponse> createPicking(@RequestBody PickingRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pickingService.createPicking(req));
    }

    // ─── State transitions ───────────────────────────────────────────────

    @PostMapping("/pickings/{id}/confirm")
    public ResponseEntity<PickingResponse> confirmPicking(@PathVariable Long id) {
        return ResponseEntity.ok(pickingService.confirmPicking(id));
    }

    @PostMapping("/pickings/{id}/validate")
    public ResponseEntity<PickingResponse> validatePicking(
            @PathVariable Long id,
            @RequestBody(required = false) List<ValidateLineRequest> lines) {
        return ResponseEntity.ok(pickingService.validatePicking(id, lines));
    }

    // ─── Scrap ───────────────────────────────────────────────────────────

    @PostMapping("/pickings/scrap")
    public ResponseEntity<Void> scrapStock(
            @RequestParam Long productId,
            @RequestParam Long locationId,
            @RequestParam BigDecimal quantity,
            @RequestParam String reason) {
        pickingService.scrapStock(productId, locationId, quantity, reason);
        return ResponseEntity.ok().build();
    }

    // ─── Reporting ───────────────────────────────────────────────────────

    @GetMapping("/reporting/stock")
    public ResponseEntity<List<StockLevelReportRow>> getStockReport() {
        return ResponseEntity.ok(pickingService.getStockLevelReport());
    }

    @GetMapping("/reporting/moves-history")
    public ResponseEntity<List<MoveHistoryReportRow>> getMovesHistory() {
        return ResponseEntity.ok(pickingService.getMoveHistoryReport());
    }
}
