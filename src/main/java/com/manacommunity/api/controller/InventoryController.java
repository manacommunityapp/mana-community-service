package com.manacommunity.api.controller;

import com.manacommunity.api.dto.CheckoutRequest;
import com.manacommunity.api.model.InventoryItem;
import com.manacommunity.api.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/inventory/items", "/api/asset-finance/assets"})
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<InventoryItem>> getItems() {
        return ResponseEntity.ok(inventoryService.getAllItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryItem> getItemById(@PathVariable Long id) {
        return inventoryService.getItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/qr/{qrCodeId}")
    public ResponseEntity<InventoryItem> getItemByQrCode(@PathVariable String qrCodeId) {
        return inventoryService.getItemByQrCode(qrCodeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<InventoryItem> createItem(@RequestBody InventoryItem item) {
        return ResponseEntity.ok(inventoryService.createItem(item));
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<InventoryItem> checkoutItem(@PathVariable Long id, @RequestBody CheckoutRequest request) {
        return inventoryService.checkoutItem(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }

    @PostMapping("/{id}/checkin")
    public ResponseEntity<InventoryItem> checkinItem(@PathVariable Long id) {
        return inventoryService.checkinItem(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<InventoryItem> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return inventoryService.updateStatus(id, status)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/maintenance")
    public ResponseEntity<InventoryItem> recordMaintenance(
            @PathVariable Long id,
            @RequestParam(required = false) String nextDueDate) {
        return inventoryService.recordMaintenance(id, nextDueDate)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/audit")
    public ResponseEntity<InventoryItem> recordAudit(
            @PathVariable Long id,
            @RequestParam int expectedQuantity,
            @RequestParam int actualQuantity) {
        return inventoryService.recordAudit(id, expectedQuantity, actualQuantity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
