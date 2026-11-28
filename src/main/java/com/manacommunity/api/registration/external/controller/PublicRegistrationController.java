package com.manacommunity.api.registration.external.controller;

import com.manacommunity.api.registration.external.model.PublicRegistrationSubmissionRequest;
import com.manacommunity.api.registration.external.model.PublicRegistrationSubmissionResponse;
import com.manacommunity.api.registration.external.model.SignedRegistrationToken;
import com.manacommunity.api.registration.external.service.ExternalRegistrationLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public endpoint for prospective participants registering from outside using a signed link.
 * Does not require prior user authentication.
 */
@RestController
@RequestMapping("/api/public/registration")
@RequiredArgsConstructor
public class PublicRegistrationController {

    private final ExternalRegistrationLinkService linkService;

    /**
     * Validates the signed link when the user lands on the registration web page.
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateLink(@RequestParam String token) {
        SignedRegistrationToken parsed = linkService.validateLink(token);
        return ResponseEntity.ok(Map.of(
                "status", "VALID",
                "tokenId", parsed.tokenId(),
                "communityId", parsed.communityId(),
                "scope", parsed.scope().name(),
                "referenceId", parsed.referenceId() != null ? parsed.referenceId() : 0L,
                "scopeTitle", parsed.scopeTitle(),
                "expiresAt", parsed.expiresAt().toString(),
                "metadata", parsed.metadata()
        ));
    }

    /**
     * Submits the registration payload signed by the HMAC token.
     */
    @PostMapping("/submit")
    public ResponseEntity<PublicRegistrationSubmissionResponse> submit(
            @Valid @RequestBody PublicRegistrationSubmissionRequest request
    ) {
        PublicRegistrationSubmissionResponse response = linkService.submitRegistration(request);
        return ResponseEntity.ok(response);
    }
}
