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
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Events', 'Manage Food Events', 'View Events', 'View Food Profile', 'View Food Menu', 'View Food Catering')")
    public ResponseEntity<?> list(@AuthenticationPrincipal UserPrincipal principal,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(eventService.list(communityId, status, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Events', 'Manage Food Events', 'View Events', 'View Food Profile')")
    public ResponseEntity<?> getById(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(eventService.getById(communityId, id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','FOOD_ADMIN') or hasAnyAuthority('Manage Food Events', 'Create Event', 'Manage Event Forms')")
    public ResponseEntity<?> create(@AuthenticationPrincipal UserPrincipal principal,
                                    @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(communityId, request, user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','FOOD_ADMIN') or hasAnyAuthority('Manage Food Events', 'Create Event', 'Manage Event Forms')")
    public ResponseEntity<?> update(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long id,
                                    @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(eventService.update(communityId, id, request));
    }

    @PostMapping("/{id}/register")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('Manage Food Events', 'View Food Events', 'Register Event', 'View Events')")
    public ResponseEntity<?> register(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long id,
                                      @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(eventService.register(communityId, id, request, user));
    }

    @GetMapping("/{id}/registrations")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('View Food Events', 'Manage Food Events', 'View Events')")
    public ResponseEntity<?> getRegistrations(@AuthenticationPrincipal UserPrincipal principal,
                                              @PathVariable Long id,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(eventService.getRegistrations(communityId, id, PageRequest.of(page, size)));
    }

    @PostMapping("/registrations/{id}/check-in")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','FOOD_ADMIN') or hasAnyAuthority('Manage Food Events', 'View Events', 'Manage Event Registration')")
    public ResponseEntity<?> checkIn(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(eventService.checkIn(communityId, id));
    }

    @PostMapping("/{id}/contributions")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('Manage Food Events', 'View Food Events', 'View Events')")
    public ResponseEntity<?> addContribution(@AuthenticationPrincipal UserPrincipal principal,
                                             @PathVariable Long id,
                                             @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(eventService.addContribution(communityId, id, request, user));
    }

    @PostMapping("/{id}/feedback")
    @PreAuthorize("hasAnyRole('ADMIN','COMMUNITY_ADMIN','SUPER_ADMIN','EVENT_ADMIN','FOOD_ADMIN','USER','RESIDENT') or hasAnyAuthority('Manage Food Events', 'View Food Events', 'View Events')")
    public ResponseEntity<?> submitFeedback(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable Long id,
                                            @RequestBody Map<String, Object> request) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
        return ResponseEntity.ok(eventService.submitFeedback(communityId, id, request, user));
    }
}
