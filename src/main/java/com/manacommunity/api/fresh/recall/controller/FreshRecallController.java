package com.manacommunity.api.fresh.recall.controller;

import com.manacommunity.api.fresh.recall.dto.InitiateRecallRequest;
import com.manacommunity.api.fresh.recall.dto.ProductRecallDto;
import com.manacommunity.api.fresh.recall.service.FreshRecallService;
import com.manacommunity.api.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fresh/recalls")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class FreshRecallController {

    private final FreshRecallService recallService;

    @PostMapping("/initiate")
    public ResponseEntity<ProductRecallDto> initiateRecall(
            @Valid @RequestBody InitiateRecallRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (principal != null && principal.getId() != null) {
            req.setInitiatedByUserId(principal.getId());
        }
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
