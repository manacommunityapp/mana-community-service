package com.manacommunity.api.commerce.core.controller;

import com.manacommunity.api.commerce.core.dto.CommerceCommissionRuleDto;
import com.manacommunity.api.commerce.core.dto.CommerceCommissionRuleRequest;
import com.manacommunity.api.commerce.core.service.CommerceCommissionService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/commerce/commission-rules")
@RequiredArgsConstructor
public class CommerceCommissionController {

    private final CommerceCommissionService commissionService;
    private final LoggedInUserService loggedInUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<List<CommerceCommissionRuleDto>> getRules(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commissionService.getAllRules(user));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<CommerceCommissionRuleDto> createRule(
            @RequestBody CommerceCommissionRuleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commissionService.createRule(user, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<CommerceCommissionRuleDto> updateRule(
            @PathVariable Long id,
            @RequestBody CommerceCommissionRuleRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(commissionService.updateRule(user, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<Void> deleteRule(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        commissionService.deleteRule(user, id);
        return ResponseEntity.ok().build();
    }
}
