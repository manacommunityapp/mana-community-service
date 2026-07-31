package com.manacommunity.api.food.controller;

import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import com.manacommunity.api.food.service.FoodEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/food/events")
@RequiredArgsConstructor
public class FoodEventController {

    private final FoodEventService eventService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('View Food Events')")
    public ResponseEntity<?> list(@AuthenticationPrincipal UserPrincipal principal,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(eventService.list(communityId, status, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('View Food Events')")
    public ResponseEntity<?> getById(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(eventService.getById(communityId, id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Manage Food Events')")
    public ResponseEntity<?> create(@AuthenticationPrincipal UserPrincipal principal,
                                    @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(communityId, request, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Food Events')")
    public ResponseEntity<?> update(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long id,
                                    @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(eventService.update(communityId, id, request));
    }

    @PostMapping("/{id}/register")
    @PreAuthorize("hasAuthority('Manage Food Events')")
    public ResponseEntity<?> register(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long id,
                                      @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(eventService.register(communityId, id, request, user));
    }

    @GetMapping("/{id}/registrations")
    @PreAuthorize("hasAuthority('View Food Events')")
    public ResponseEntity<?> getRegistrations(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long id,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(eventService.getRegistrations(communityId, id, PageRequest.of(page, size)));
    }

    @PostMapping("/registrations/{id}/check-in")
    @PreAuthorize("hasAuthority('Manage Food Events')")
    public ResponseEntity<?> checkIn(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(eventService.checkIn(communityId, id));
    }

    @PostMapping("/{id}/contributions")
    @PreAuthorize("hasAuthority('Manage Food Events')")
    public ResponseEntity<?> addContribution(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long id,
                                             @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(eventService.addContribution(communityId, id, request, user));
    }

    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasAuthority('Manage Food Events')")
    public ResponseEntity<?> submitFeedback(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable Long id,
                                            @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user.getCommunity().getId();
        return ResponseEntity.ok(eventService.submitFeedback(communityId, id, request, user));
    }
}
