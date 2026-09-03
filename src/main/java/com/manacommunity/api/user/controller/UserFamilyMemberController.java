package com.manacommunity.api.user.controller;

import com.manacommunity.api.user.dto.FamilyMemberSlimResponse;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.model.FamilyMember;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.FamilyMemberService;
import com.manacommunity.api.user.service.LoggedInUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/users/family-members", "/api/family-members"})
public class UserFamilyMemberController {

    private final FamilyMemberService service;
    private final LoggedInUserService loggedInUserService;

    public UserFamilyMemberController(FamilyMemberService service, LoggedInUserService loggedInUserService) {
        this.service = service;
        this.loggedInUserService = loggedInUserService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FamilyMember>> getFamilyMembers(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = (principal != null) ? principal.getCommunityId() : null;
        List<FamilyMember> list = service.getFamilyMembers(user, communityId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/slim")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FamilyMemberSlimResponse>> getSlimFamilyMembers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long userId) {
        Long targetId = (userId != null) ? userId : (principal != null ? principal.getId() : null);
        return ResponseEntity.ok(service.getSlimFamilyMembers(targetId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FamilyMember> addFamilyMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody FamilyMember member) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = (principal != null) ? principal.getCommunityId() : null;
        FamilyMember created = service.addFamilyMember(member, user, communityId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FamilyMember> updateFamilyMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody FamilyMember member) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = (principal != null) ? principal.getCommunityId() : null;
        FamilyMember updated = service.updateFamilyMember(id, member, user, communityId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteFamilyMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        AppUser user = loggedInUserService.resolve(principal);
        Long communityId = (principal != null) ? principal.getCommunityId() : null;
        service.deleteFamilyMember(id, user, communityId);
        return ResponseEntity.noContent().build();
    }
}
