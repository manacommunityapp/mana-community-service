package com.manacommunity.api.healthcare.controller;

import com.manacommunity.api.healthcare.entity.ProviderCredential;
import com.manacommunity.api.healthcare.service.ProviderCredentialService;
import com.manacommunity.api.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/healthcare/credentials")
@RequiredArgsConstructor
public class ProviderCredentialController {

    private final ProviderCredentialService credentialService;

    @PostMapping
    public ResponseEntity<ProviderCredential> add(
            @RequestBody ProviderCredential cred,
            @AuthenticationPrincipal UserPrincipal principal) {
        cred.setCommunityId(principal.getCommunityId());
        return ResponseEntity.status(201).body(credentialService.addCredential(cred));
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<ProviderCredential> verify(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(credentialService.verify(id, principal.getUserId()));
    }

    @GetMapping
    public ResponseEntity<List<ProviderCredential>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(credentialService.getByCommunity(principal.getCommunityId()));
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<ProviderCredential>> byProvider(
            @PathVariable Long providerId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(credentialService.getByProvider(principal.getCommunityId(), providerId));
    }
}
