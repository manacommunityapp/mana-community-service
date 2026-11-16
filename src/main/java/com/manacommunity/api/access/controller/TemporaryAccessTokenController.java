package com.manacommunity.api.access.controller;

import com.manacommunity.api.access.entity.TemporaryAccessToken;
import com.manacommunity.api.access.service.TemporaryAccessTokenService;
import com.manacommunity.api.user.model.AppUser;
import com.manacommunity.api.user.security.UserPrincipal;
import com.manacommunity.api.user.service.LoggedInUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/access/temp-passes")
@RequiredArgsConstructor
public class TemporaryAccessTokenController {

    private final TemporaryAccessTokenService service;
    private final LoggedInUserService loggedInUserService;

    @PostMapping
    public ResponseEntity<TemporaryAccessToken> createPass(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CreatePassRequest request) {
        AppUser caller = loggedInUserService.resolve(principal);
        TemporaryAccessToken token = service.createPass(
                caller.getCommunity().getId(), caller.getId(),
                request.getPurpose(), request.getVisitorName(),
                request.getValidFrom(), request.getValidUntil(), request.getMaxUses());
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    @GetMapping
    public ResponseEntity<List<TemporaryAccessToken>> getMyPasses(
            @AuthenticationPrincipal UserPrincipal principal) {
        AppUser caller = loggedInUserService.resolve(principal);
        return ResponseEntity.ok(service.getMyPasses(caller.getId()));
    }

    @PostMapping("/verify")
    public ResponseEntity<TemporaryAccessToken> verifyPass(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.verifyPass(body.get("tokenCode")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokePass(@PathVariable Long id) {
        service.revokePass(id);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class CreatePassRequest {
        private String purpose;
        private String visitorName;
        private LocalDateTime validFrom;
        private LocalDateTime validUntil;
        private Integer maxUses;
    }
}
