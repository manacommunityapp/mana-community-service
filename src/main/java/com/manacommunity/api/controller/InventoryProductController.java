package com.manacommunity.api.controller;

import com.manacommunity.api.dto.InventoryProductRequest;
import com.manacommunity.api.dto.InventoryProductResponse;
import com.manacommunity.api.service.InventoryProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/products")
@RequiredArgsConstructor
public class InventoryProductController {

    private final InventoryProductService productService;

    @GetMapping
    public ResponseEntity<List<InventoryProductResponse>> getAll() {
        return ResponseEntity.ok(productService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InventoryProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<InventoryProductResponse>> search(@RequestParam String q) {
        return ResponseEntity.ok(productService.search(q));
    }

    @PostMapping
    public ResponseEntity<InventoryProductResponse> create(
            @Valid @RequestBody InventoryProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InventoryProductResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody InventoryProductRequest req) {
        return ResponseEntity.ok(productService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
