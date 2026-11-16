package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventBudgetRequest;
import com.manacommunity.api.events.dto.EventBudgetResponse;
import com.manacommunity.api.events.service.EventBudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/budgets")
@RequiredArgsConstructor
public class EventBudgetController {

    private final EventBudgetService budgetService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN')")
    public ResponseEntity<List<EventBudgetResponse>> listByCommunity(
            @RequestParam Long communityId) {
        return ResponseEntity.ok(budgetService.listByCommunity(communityId));
    }

    @GetMapping("/event/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN')")
    public ResponseEntity<EventBudgetResponse> getByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(budgetService.getByEvent(eventId));
    }

    @PutMapping("/event/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN')")
    public ResponseEntity<EventBudgetResponse> createOrUpdate(
            @PathVariable Long eventId,
            @Valid @RequestBody EventBudgetRequest request) {
        return ResponseEntity.ok(budgetService.createOrUpdate(eventId, request));
    }

    @DeleteMapping("/event/{eventId}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long eventId) {
        budgetService.delete(eventId);
        return ResponseEntity.noContent().build();
    }
}
