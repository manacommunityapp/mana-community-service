package com.manacommunity.api.family.controller;

import com.manacommunity.api.family.dto.FamilyMemberRequest;
import com.manacommunity.api.family.service.FamilyService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.model.FamilyMember;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/family")
@RequiredArgsConstructor
public class FamilyController {

    private final FamilyService familyService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping("/members")
    public ResponseEntity<List<FamilyMember>> getMyFamily(@AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(familyService.getMyFamily(user));
    }

    @PostMapping("/members")
    public ResponseEntity<FamilyMember> addMember(@RequestBody FamilyMemberRequest req,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(familyService.addMember(req, user));
    }

    @PutMapping("/members/{id}")
    public ResponseEntity<FamilyMember> updateMember(@PathVariable Long id,
                                                      @RequestBody FamilyMemberRequest req,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(familyService.updateMember(id, req, user));
    }

    @DeleteMapping("/members/{id}")
    public ResponseEntity<Void> removeMember(@PathVariable Long id,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        familyService.removeMember(id, user);
        return ResponseEntity.noContent().build();
    }
}
