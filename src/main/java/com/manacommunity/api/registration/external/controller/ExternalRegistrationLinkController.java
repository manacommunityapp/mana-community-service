package com.manacommunity.api.registration.external.controller;

import com.manacommunity.api.registration.external.model.GenerateRegistrationLinkRequest;
import com.manacommunity.api.registration.external.model.GenerateRegistrationLinkResponse;
import com.manacommunity.api.registration.external.service.ExternalRegistrationLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin API to create cryptographically signed registration links for tournaments and public onboarding.
 */
@RestController
@RequestMapping("/api/registration-links")
@RequiredArgsConstructor
public class ExternalRegistrationLinkController {

    private final ExternalRegistrationLinkService linkService;

    @PostMapping("/generate")
    public ResponseEntity<GenerateRegistrationLinkResponse> generateLink(
            @Valid @RequestBody GenerateRegistrationLinkRequest request
    ) {
        GenerateRegistrationLinkResponse response = linkService.generateLink(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
