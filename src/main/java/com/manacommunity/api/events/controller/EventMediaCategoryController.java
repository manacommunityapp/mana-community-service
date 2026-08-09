package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.dto.EventMediaCategoryRequest;
import com.manacommunity.api.events.dto.EventMediaCategoryResponse;
import com.manacommunity.api.events.service.EventMediaCategoryService;
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

@RestController
@RequestMapping("/api/events/media-categories")
@RequiredArgsConstructor
public class EventMediaCategoryController {

    private final EventMediaCategoryService categoryService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Events')")
    public ResponseEntity<List<EventMediaCategoryResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(categoryService.getAll(user.getCommunity().getId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<EventMediaCategoryResponse> create(
            @Valid @RequestBody EventMediaCategoryRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.create(req, user.getCommunity()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Create Event')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        categoryService.delete(id, user.getCommunity().getId());
        return ResponseEntity.noContent().build();
    }
}
