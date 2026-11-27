package com.manacommunity.api.food.controller;

import com.manacommunity.api.food.entity.DeliveryBatch;
import com.manacommunity.api.food.entity.DeliveryBatchOrder;
import com.manacommunity.api.food.service.DeliveryBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/food/batches")
@RequiredArgsConstructor
public class DeliveryBatchController {

    private final DeliveryBatchService batchService;

    @PostMapping("/slot/{slotId}/create-batches")
    public ResponseEntity<List<DeliveryBatch>> createBatches(
            @PathVariable Long slotId,
            @RequestBody List<DeliveryBatchOrder> orders) {
        return ResponseEntity.ok(batchService.createBatches(slotId, orders));
    }

    @GetMapping("/slot/{slotId}")
    public ResponseEntity<List<DeliveryBatch>> getBatches(@PathVariable Long slotId) {
        return ResponseEntity.ok(batchService.getBatchesForSlot(slotId));
    }

    @PutMapping("/{batchId}/assign")
    public ResponseEntity<Void> assign(@PathVariable Long batchId, @RequestBody Map<String, Long> body) {
        batchService.assignDeliveryPerson(batchId, body.get("userId"));
        return ResponseEntity.ok().build();
    }

    @PutMapping("/order/{batchOrderId}/delivered")
    public ResponseEntity<Void> markDelivered(@PathVariable Long batchOrderId, @RequestBody Map<String, String> body) {
        batchService.markDelivered(batchOrderId, body.getOrDefault("notes", null));
        return ResponseEntity.ok().build();
    }
}
