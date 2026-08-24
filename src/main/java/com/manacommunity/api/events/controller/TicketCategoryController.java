package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.EventTicketCategoryMaster;
import com.manacommunity.api.events.service.TicketCategoryService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events/ticket-categories")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class TicketCategoryController {

    private final TicketCategoryService ticketCategoryService;

    @GetMapping
    public ResponseEntity<List<EventTicketCategoryMaster>> getCategories(
            @AuthenticationPrincipal UserPrincipal principal) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        return ResponseEntity.ok(ticketCategoryService.getCategories(communityId));
    }

    @PostMapping
    public ResponseEntity<EventTicketCategoryMaster> createCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long communityId = principal != null ? principal.getCommunityId() : null;
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        Integer displayOrder = body.get("displayOrder") != null
                ? Integer.parseInt(body.get("displayOrder").toString())
                : 0;

        EventTicketCategoryMaster created = ticketCategoryService.createCategory(communityId, name, description, displayOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN','COMMUNITY_ADMIN','EVENT_ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        ticketCategoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
