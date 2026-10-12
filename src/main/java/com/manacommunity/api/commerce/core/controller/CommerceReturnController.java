package com.manacommunity.api.commerce.core.controller;

import com.manacommunity.api.commerce.core.dto.CommerceReturnDto;
import com.manacommunity.api.commerce.core.dto.CommerceReturnRequest;
import com.manacommunity.api.commerce.core.service.CommerceReturnService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/commerce/returns")
@RequiredArgsConstructor
public class CommerceReturnController {

    private final CommerceReturnService returnService;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    public ResponseEntity<CommerceReturnDto> create(
            @RequestBody CommerceReturnRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(returnService.createReturn(user, req));
    }

    @GetMapping("/my")
    public ResponseEntity<List<CommerceReturnDto>> getMy(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(returnService.getMyReturns(user));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<List<CommerceReturnDto>> getAdmin(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(returnService.getAdminReturns(user, status));
    }

    @PostMapping("/{returnCode}/resolve")
    @PreAuthorize("hasAuthority('Manage Commerce Orders')")
    public ResponseEntity<CommerceReturnDto> resolve(
            @PathVariable String returnCode,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser user = loggedInUserService.resolve(principal);
        String status = body.getOrDefault("status", "APPROVED");
        String note = body.getOrDefault("adminNote", "");
        return ResponseEntity.ok(returnService.resolveReturn(user, returnCode, status, note));
    }
}
