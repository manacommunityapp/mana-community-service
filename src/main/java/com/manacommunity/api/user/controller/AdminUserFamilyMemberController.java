package com.manacommunity.api.user.controller;

import com.manacommunity.api.user.dto.FamilyMemberResponse;
import com.manacommunity.api.user.service.FamilyMemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/family-members")
public class AdminUserFamilyMemberController {

    private final FamilyMemberService service;

    public AdminUserFamilyMemberController(FamilyMemberService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'COMMUNITY_ADMIN')")
    public ResponseEntity<List<FamilyMemberResponse>> getFamilyMembersForUser(
            @PathVariable Long userId) {
        return ResponseEntity.ok(service.getFamilyMembersByUserId(userId));
    }
}
