package com.manacommunity.api.food.controller;

import com.manacommunity.api.food.entity.ProductSubstitute;
import com.manacommunity.api.food.service.ProductStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/food/stock")
@RequiredArgsConstructor
public class ProductStockController {

    private final ProductStockService stockService;

    @PostMapping("/out-of-stock")
    public ResponseEntity<Void> markOutOfStock(@RequestBody Map<String, Object> body) {
        stockService.markOutOfStock(
            Long.parseLong(body.get("vendorId").toString()),
            Long.parseLong(body.get("productId").toString()),
            body.get("productType").toString(),
            Long.parseLong(body.get("communityId").toString()),
            body.containsKey("updatedBy") ? Long.parseLong(body.get("updatedBy").toString()) : null,
            body.containsKey("restockDate") ? LocalDate.parse(body.get("restockDate").toString()) : null
        );
        return ResponseEntity.ok().build();
    }

    @PostMapping("/in-stock")
    public ResponseEntity<Void> markInStock(@RequestBody Map<String, Object> body) {
        stockService.markInStock(
            Long.parseLong(body.get("vendorId").toString()),
            Long.parseLong(body.get("productId").toString()),
            body.get("productType").toString(),
            body.containsKey("updatedBy") ? Long.parseLong(body.get("updatedBy").toString()) : null
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/substitutes")
    public ResponseEntity<List<ProductSubstitute>> getSubstitutes(
            @RequestParam Long vendorId, @RequestParam Long productId, @RequestParam String productType) {
        return ResponseEntity.ok(stockService.getSubstitutes(vendorId, productId, productType));
    }

    @PostMapping("/substitutes")
    public ResponseEntity<ProductSubstitute> addSubstitute(@RequestBody ProductSubstitute sub) {
        return ResponseEntity.status(201).body(stockService.addSubstitute(sub));
    }

    @DeleteMapping("/substitutes/{id}")
    public ResponseEntity<Void> removeSubstitute(@PathVariable Long id) {
        stockService.removeSubstitute(id);
        return ResponseEntity.noContent().build();
    }
}
