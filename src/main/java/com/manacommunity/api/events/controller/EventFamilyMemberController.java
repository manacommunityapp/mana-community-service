package com.manacommunity.api.events.controller;

import com.manacommunity.api.events.entity.EventFamilyMember;
import com.manacommunity.api.events.service.EventFamilyMemberService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events/family-members")
public class EventFamilyMemberController {

    private final EventFamilyMemberService service;
    private final LoggedInUserService loggedInUserService;

    public EventFamilyMemberController(EventFamilyMemberService service, LoggedInUserService loggedInUserService) {
        this.service = service;
        this.loggedInUserService = loggedInUserService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('View Events', 'Register Event', 'View Event Dashboard') or hasAnyRole('USER', 'MEMBER', 'ADMIN', 'COMMUNITY_ADMIN', 'EVENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<EventFamilyMember>> getFamilyMembers(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = (principal != null) ? principal.getCommunityId() : null;
        List<EventFamilyMember> list = service.getFamilyMembers(user, communityId);
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('View Events', 'Register Event', 'View Event Dashboard') or hasAnyRole('USER', 'MEMBER', 'ADMIN', 'COMMUNITY_ADMIN', 'EVENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<EventFamilyMember> addFamilyMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody EventFamilyMember member) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = (principal != null) ? principal.getCommunityId() : null;
        EventFamilyMember created = service.addFamilyMember(member, user, communityId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('View Events', 'Register Event', 'View Event Dashboard') or hasAnyRole('USER', 'MEMBER', 'ADMIN', 'COMMUNITY_ADMIN', 'EVENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<EventFamilyMember> updateFamilyMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody EventFamilyMember member) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = (principal != null) ? principal.getCommunityId() : null;
        EventFamilyMember updated = service.updateFamilyMember(id, member, user, communityId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('View Events', 'Register Event', 'View Event Dashboard') or hasAnyRole('USER', 'MEMBER', 'ADMIN', 'COMMUNITY_ADMIN', 'EVENT_ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deleteFamilyMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = (principal != null) ? principal.getCommunityId() : null;
        service.deleteFamilyMember(id, user, communityId);
        return ResponseEntity.noContent().build();
    }
}
