package com.manacommunity.api.fresh.recall.controller;

import com.manacommunity.api.fresh.recall.dto.InitiateRecallRequest;
import com.manacommunity.api.fresh.recall.dto.ProductRecallDto;
import com.manacommunity.api.fresh.recall.service.FreshRecallService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fresh/recalls")
@RequiredArgsConstructor
public class FreshRecallController {

    private final FreshRecallService recallService;

    @PostMapping("/initiate")
    public ResponseEntity<ProductRecallDto> initiateRecall(@Valid @RequestBody InitiateRecallRequest req) {
        return ResponseEntity.ok(recallService.initiateRecall(req));
    }

    @GetMapping
    public ResponseEntity<List<ProductRecallDto>> getAllRecalls() {
        return ResponseEntity.ok(recallService.getAllRecalls());
    }

    @GetMapping("/{recallId}")
    public ResponseEntity<ProductRecallDto> getRecallById(@PathVariable Long recallId) {
        return ResponseEntity.ok(recallService.getRecallById(recallId));
    }

    @PostMapping("/{recallId}/process-refunds")
    public ResponseEntity<ProductRecallDto> processRecallRefunds(@PathVariable Long recallId) {
        return ResponseEntity.ok(recallService.processRecallRefunds(recallId));
    }
}
