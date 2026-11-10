package com.manacommunity.api.freshkitchen.controller;

import com.manacommunity.api.freshkitchen.dto.BreakfastComboDto;
import com.manacommunity.api.freshkitchen.dto.CustomizeComboRequest;
import com.manacommunity.api.freshkitchen.dto.CustomizedComboResponseDto;
import com.manacommunity.api.freshkitchen.dto.FreshProductDto;
import com.manacommunity.api.freshkitchen.model.FoodCategory;
import com.manacommunity.api.freshkitchen.service.FreshKitchenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/fresh-kitchen", "/fresh-kitchen"})
@RequiredArgsConstructor
public class FreshKitchenController {

    private final FreshKitchenService freshKitchenService;

    @GetMapping("/products")
    public ResponseEntity<List<FreshProductDto>> getAllProducts(
            @RequestParam(required = false) FoodCategory category) {
        if (category != null) {
            return ResponseEntity.ok(freshKitchenService.getProductsByCategory(category));
        }
        return ResponseEntity.ok(freshKitchenService.getAllProducts());
    }

    @GetMapping("/batters")
    public ResponseEntity<List<FreshProductDto>> getBatters() {
        return ResponseEntity.ok(freshKitchenService.getBatters());
    }

    @GetMapping("/products/{productId}")
    public ResponseEntity<FreshProductDto> getProduct(@PathVariable String productId) {
        return ResponseEntity.ok(freshKitchenService.getProductByProductId(productId));
    }

    @GetMapping("/combos")
    public ResponseEntity<List<BreakfastComboDto>> getAllCombos() {
        return ResponseEntity.ok(freshKitchenService.getAllBreakfastCombos());
    }

    @GetMapping("/combos/{comboCode}")
    public ResponseEntity<BreakfastComboDto> getCombo(@PathVariable String comboCode) {
        return ResponseEntity.ok(freshKitchenService.getBreakfastComboByCode(comboCode));
    }

    @PostMapping("/combos/customize")
    public ResponseEntity<CustomizedComboResponseDto> customizeCombo(
            @Valid @RequestBody CustomizeComboRequest request) {
        return ResponseEntity.ok(freshKitchenService.customizeCombo(request));
    }
}
