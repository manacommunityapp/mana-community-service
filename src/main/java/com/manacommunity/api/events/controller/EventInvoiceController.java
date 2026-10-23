package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventInvoiceRequest;
import com.manacommunity.api.events.dto.EventInvoiceResponse;
import com.manacommunity.api.events.service.EventInvoiceService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events/invoices")
@RequiredArgsConstructor
public class EventInvoiceController {

    private final EventInvoiceService invoiceService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','USER','RESIDENT') or hasAuthority('View Events')")
    public ResponseEntity<List<EventInvoiceResponse>> getAll(
            @RequestParam(required = false) Long eventId,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (eventId != null) {
            return ResponseEntity.ok(invoiceService.getByEvent(eventId));
        }
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity() != null ? user.getCommunity().getId() : null;
        if (communityId == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(invoiceService.getByCommunity(communityId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<EventInvoiceResponse> create(
            @Valid @RequestBody EventInvoiceRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.create(req, user, user.getCommunity()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<EventInvoiceResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventInvoiceRequest req) {
        return ResponseEntity.ok(invoiceService.update(id, req));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<EventInvoiceResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(invoiceService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN') or hasAuthority('Create Event')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
