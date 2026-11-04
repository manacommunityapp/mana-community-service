package com.manacommunity.api.fresh.weight.controller;

import com.manacommunity.api.fresh.weight.dto.RecordWeightRequest;
import com.manacommunity.api.fresh.weight.dto.WeightAdjustmentDto;
import com.manacommunity.api.fresh.weight.service.FreshWeightAdjustmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fresh/weight")
@RequiredArgsConstructor
public class FreshWeightController {

    private final FreshWeightAdjustmentService weightAdjustmentService;

    @PostMapping("/adjust")
    public ResponseEntity<WeightAdjustmentDto> recordActualWeight(@Valid @RequestBody RecordWeightRequest req) {
        return ResponseEntity.ok(weightAdjustmentService.recordActualWeight(req));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<WeightAdjustmentDto>> getAdjustmentsByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(weightAdjustmentService.getAdjustmentsByOrder(orderId));
    }

    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<WeightAdjustmentDto>> getAdjustmentsByItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(weightAdjustmentService.getAdjustmentsByItem(itemId));
    }
}
